package minesweeper.bulk;

import minesweeper.gamestate.GameFactory;
import minesweeper.gamestate.GameStateModel;
import minesweeper.random.DefaultRNG;
import minesweeper.random.RNG;
import minesweeper.settings.GameSettings;
import minesweeper.settings.GameType;
import minesweeper.solver.Solver;
import minesweeper.solver.settings.SolverSettings;
import minesweeper.structure.Action;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * Re-implementation of {@link minesweeper.solver.bulk.BulkController} and {@link minesweeper.solver.bulk.BulkPlayer}.
 * Difference: Remove secondary figures (ws, mastery, fairness, ...) from original BulkController,
 * remove UI-related functionalities,
 * move them to modularized secondary class (BulkEvent?),
 * and add custom termination condition.
 */

public class ExtendedBulk implements Runnable {
    private final GameSettings gameSettings;
    private final GameType gameType;

    private final int workers;
    private final int bufferSize;
    private final ExtendedRequest[] buffer;
    private final ExtendedWorker[] bulkWorkers;
    private List<Action> preActions;

    private final static int REPORT_INTERVAL = 200;
    private final static int DEFAULT_BUFFER_PER_WORKER = 1000;

    private volatile AtomicInteger waitingSlot = new AtomicInteger(-1);   // this is the next slot we are waiting to be returned
    private volatile int nextSlot = 0;      // this is the next slot to be dispatched

    private volatile int nextSequence = 1;
    private volatile AtomicInteger waitingSequence = new AtomicInteger(0);
    private final RNG seeder;
    private volatile boolean finished = false;

    private long startTime;
    private long endTime;
    private ScheduledExecutorService executor;

    Function<ExtendedConsumer, Boolean> endCondition;
    Function<GameStateModel, ? extends Solver> solverFunction;
    public ExtendedConsumer consumer;

    public ExtendedBulk(long seed, Function<ExtendedConsumer, Boolean> endCondition, GameType gameType, GameSettings gameSettings, Function<GameStateModel, ? extends Solver> solverFunction, int workers) {
        this(seed, endCondition, gameType, gameSettings, solverFunction, workers, DEFAULT_BUFFER_PER_WORKER);
    }

    public ExtendedBulk(long seed, Function<ExtendedConsumer, Boolean> endCondition, GameType gameType, GameSettings gameSettings, Function<GameStateModel, ? extends Solver> solverFunction, int workers, int bufferPerWorker) {
        this.gameType = gameType;
        this.gameSettings = gameSettings;
        this.endCondition = endCondition;
        this.seeder = DefaultRNG.getRNG(seed);
        this.workers = workers;
        this.bulkWorkers = new ExtendedWorker[this.workers];
        this.solverFunction = solverFunction;

        this.bufferSize = bufferPerWorker * this.workers;
        this.buffer = new ExtendedRequest[bufferSize];
        this.preActions = Collections.emptyList();
    }

    public void registerConsumer(ExtendedConsumer consumer) {
        this.consumer = consumer;
    }

    /**
     * Start the number of workers and wait for them to complete. If you don't want your main thread paused then run this on a separate thread.
     */
    @Override
    public void run() {
        this.startTime = System.currentTimeMillis();


        for (int i=0; i < workers; i++) {
            bulkWorkers[i] = new ExtendedWorker(this, i);
            new Thread(bulkWorkers[i], "worker-" + (i+1)).start();
        }

        executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(() -> {
            System.out.println("Main thread waiting for bulk run to complete...");
            if (finished) {
                executor.shutdown();
                this.endTime = System.currentTimeMillis();
                System.out.println("Finished after " + getDuration() + " milliseconds");
            }
            // TODO consumer.print sometimes?
        }, 10, 10, TimeUnit.SECONDS);
    }

    /**
     *
     * @return how long the bulk run took in milliseconds, or how long it has been running depending if it has finished ot not
     */
    public long getDuration() {

        if (startTime == 0) {  // not started
            return 0;
        } else if (finished && endTime != 0) {  // finished
            return endTime - startTime;
        } else {
            return System.currentTimeMillis() - startTime;  // in flight
        }

    }

    /**
     * Returns the last request and gets the next
     */
    protected synchronized ExtendedRequest getNextRequest(ExtendedRequest request) {

        if (request != null) {
            buffer[request.slot] = request;

            // if this is the slot we are waiting on then process the games which are in the buffer - this is all synchronised so nothing else arrives will it happens
            if (request.slot == waitingSlot.get()) {
                processSlots();
            }
        }

        // if we have played all the games or we have been stopped then tell the workers to stop
        if (this.endCondition.apply(this.consumer) || finished) {
            return ExtendedRequest.STOP;
        }

        // if the next sequence is a long way ahead of the waiting sequence then wait until we catch up.  Tell the worker to wait.
        if (nextSequence > waitingSequence.get() + bufferSize - 2) {
            //System.out.println("Buffer is full after " + nextSequence + " games dispatched");
            return ExtendedRequest.WAIT;
        }
        ExtendedRequest next = new ExtendedRequest();
        next.action = ExtendedRequest.BulkAction.RUN;
        next.sequence = this.nextSequence;
        next.slot = this.nextSlot;
        next.gs = getGameState(seeder);

        // roll onto the next sequence
        this.nextSequence++;
        this.nextSlot++;

        // if this is the first request then initialise the waiting slot
        if (waitingSlot.get() == -1) {
            waitingSlot.incrementAndGet();
        }

        // recycle the buffer when we get beyond the top
        if (this.nextSlot >= bufferSize) {
            this.nextSlot = this.nextSlot - bufferSize;
        }

        return next;
    }

    public void setPreActions(List<Action> actions) {
        this.preActions = actions;
    }

    protected GameStateModel getGameState(RNG seeder) {
        // play the pre-actions while not dead
        while (true) {
            GameStateModel gs = GameFactory.create(this.gameType, this.gameSettings, seeder.random(0));
            for (Action a: preActions) {
                gs.doAction(a);
                if (gs.getGameState() == GameStateModel.LOST) {
                    break;
                }
            }
            if (gs.getGameState() != GameStateModel.LOST) {
                return gs;
            }
        }
    }

    private void processSlots() {
        // process all the games which have been processed and are waiting in the buffer
        while (buffer[waitingSlot.get()] != null) {
            ExtendedRequest request = buffer[waitingSlot.get()];
            consumer.processRequest(request);
            // clear the buffer and move on to the next slot
            buffer[waitingSlot.get()] = null;
            waitingSlot.incrementAndGet();
            waitingSequence.incrementAndGet();

            // recycle the buffer when we get beyond the top
            if (this.waitingSlot.get() >= bufferSize) {
                this.waitingSlot.addAndGet(-bufferSize);
            }

            // if we have run and processed all the games then wake the main thread
            if (endCondition.apply(this.consumer)) {
                System.out.println("All games played, exiting the main thread");
                finished = true;
                executor.shutdown();
                System.out.println(this.consumer.print());
            }
        }
    }
}
