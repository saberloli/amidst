package amidst.threading;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.swing.SwingUtilities;

import amidst.documentation.AmidstThread;
import amidst.documentation.CalledOnlyBy;
import amidst.documentation.ThreadSafe;

@ThreadSafe
public class ThreadMaster {
	private static final Runnable NOOP = new Runnable() {
		public void run() {
			// noop
		}
	};

	private final ScheduledExecutorService repaintExecutorService;
	private final ScheduledExecutorService fragmentLoaderExecutorService;
	private final ExecutorService workerExecutorService;
	private final WorkerExecutor workerExecutor;

	private volatile Runnable onRepaintTick;
	private volatile Runnable onFragmentLoadTick;

	public ThreadMaster() {
		this.repaintExecutorService = createRepaintExecutorService();
		this.fragmentLoaderExecutorService = createFragmentLoaderExecutorService();
		this.workerExecutorService = createWorkerExecutorService();
		this.workerExecutor = createWorkerExecutor();
		this.onRepaintTick = NOOP;
		this.onFragmentLoadTick = NOOP;
		startRepainter();
		startFragmentLoader();
	}

	private ScheduledExecutorService createRepaintExecutorService() {
		return Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
			@Override
			public Thread newThread(Runnable r) {
				Thread thread = new Thread(r);
				thread.setDaemon(true);
				return thread;
			}
		});
	}

	private ScheduledExecutorService createFragmentLoaderExecutorService() {
		return Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
			@Override
			public Thread newThread(Runnable r) {
				Thread thread = new Thread(r);
				thread.setDaemon(true);
				thread.setPriority(Thread.MIN_PRIORITY);
				return thread;
			}
		});
	}

	private ExecutorService createWorkerExecutorService() {
		return Executors.newCachedThreadPool(new ThreadFactory() {
			@Override
			public Thread newThread(Runnable r) {
				Thread thread = new Thread(r);
				thread.setDaemon(true);
				thread.setPriority(Thread.MIN_PRIORITY);
				return thread;
			}
		});
	}

	private WorkerExecutor createWorkerExecutor() {
		return new WorkerExecutor(this);
	}

	private void startRepainter() {
		repaintExecutorService.scheduleAtFixedRate(new Runnable() {
			@CalledOnlyBy(AmidstThread.REPAINTER)
			@Override
			public void run() {
				onRepaintTick.run();
			}
		}, 0, 20, TimeUnit.MILLISECONDS);
	}

	private void startFragmentLoader() {
		fragmentLoaderExecutorService.scheduleWithFixedDelay(new Runnable() {
			@CalledOnlyBy(AmidstThread.FRAGMENT_LOADER)
			@Override
			public void run() {
				onFragmentLoadTick.run();
			}
		}, 0, 20, TimeUnit.MILLISECONDS);
	}

	public <T> void executeWorker(final Worker<T> worker) {
		workerExecutorService.execute(new Runnable() {
			@CalledOnlyBy(AmidstThread.WORKER)
			@Override
			public void run() {
				try {
					callFinishedLater(worker, worker.execute());
				} catch (Exception e) {
					callErrorLater(worker, e);
				}
			}
		});
	}

	@CalledOnlyBy(AmidstThread.WORKER)
	private <T> void callErrorLater(final Worker<T> worker, final Exception e) {
		SwingUtilities.invokeLater(new Runnable() {
			@CalledOnlyBy(AmidstThread.EDT)
			@Override
			public void run() {
				worker.error(e);
			}
		});
	}

	@CalledOnlyBy(AmidstThread.WORKER)
	private <T> void callFinishedLater(final Worker<T> worker, final T result) {
		SwingUtilities.invokeLater(new Runnable() {
			@CalledOnlyBy(AmidstThread.EDT)
			@Override
			public void run() {
				worker.finished(result);
			}
		});
	}

	public WorkerExecutor getWorkerExecutor() {
		return workerExecutor;
	}

	public void setOnRepaintTick(Runnable onRepaintTick) {
		this.onRepaintTick = onRepaintTick;
	}

	public void setOnFragmentLoadTick(Runnable onFragmentLoadTick) {
		this.onFragmentLoadTick = onFragmentLoadTick;
	}

	public void clearOnRepaintTick() {
		this.onRepaintTick = NOOP;
	}

	public void clearOnFragmentLoadTick() {
		this.onFragmentLoadTick = NOOP;
	}
}
