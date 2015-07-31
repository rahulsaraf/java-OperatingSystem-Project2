import java.util.Random;
import java.util.concurrent.Semaphore;

/**
 * @author Rahul Saraf (rss130030)
 * @this This class represents the postal office which is responsible for
 *       initializing the customer and postal workker threads.
 *
 */
public class PostOfficeSimulator {
	// Maximum number of customers for this simulation.
	int CUSTOMER_MAX_CAP = 50;
	// this is max postal officers available in office
	int POSTAL_WORKER_MAX_CAP = 3;
	int customerThreadCount=50;
	// for all 50 customers. This is to avoid interference between threads.
	Semaphore finished[] = new Semaphore[50];

	// to allow only 10 threads to enter at a time.
	Semaphore customerEntry = new Semaphore(10, true);
	// to allow only three thread at a time
	Semaphore postalWorker = new Semaphore(3, true);

	// to let postal class copy the value of global variable of customer ID and
	// action. And let customer thread copy the value of global variable postal
	// worker ID.
	Semaphore mutex1 = new Semaphore(1, true);
	Semaphore mutex2 = new Semaphore(0, true);
	Semaphore mutex3 = new Semaphore(0, true);
	Semaphore mutex4 = new Semaphore(1, true);

	// this semaphore will release after worker assigned to the customer.
	Semaphore postalWorkerAssigned = new Semaphore(0, true);
	// this semaphore is released when customer request services from worker.
	// such as mailing a letter, mailing a package, buying stamps.
	Semaphore customerRequested = new Semaphore(0, true);
	// this semaphore is used to control access on scale. At a time only one
	// worker thread can access.
	Semaphore scale = new Semaphore(1, true);

	// these are global variables to store the value of customer ID, Postal
	// Worker ID and Customer action.
	public int customerID;
	public int postalWorkerID;
	public int custActionID;

	/**
	 * This method is responsible for initializing all the threads of Customer,
	 * PostalWorker. After all threads are done, all threads will be joined.
	 * 
	 */
	public void startPostalOffice() {

		// array of 50 customer objects
		Customer custObjArray[] = new Customer[CUSTOMER_MAX_CAP];
		// array of 3 postalWorker objects
		PostalWorker worker[] = new PostalWorker[POSTAL_WORKER_MAX_CAP];

		// initialize the finished semaphore with 0 value.
		for (int i = 0; i < finished.length; i++) {
			finished[i] = new Semaphore(0);
		}

		// initialize all the customer threads.
		for (int i = 0; i < CUSTOMER_MAX_CAP; i++) {
			Customer cust = new Customer(i, new Random().nextInt(3));
			custObjArray[i] = cust;
			System.out.println("Customer " + i + " created");
			cust.start();
		}

		// initializa the postalWorker threads.
		for (int i = 0; i < POSTAL_WORKER_MAX_CAP; i++) {
			PostalWorker postalWorker = new PostalWorker(i);
			worker[i] = postalWorker;
			System.out.println("Postal Worker " + i + " created");
			postalWorker.start();
		}

		// join all the customer threads once the processing is done.
		for (int i = 0; i < CUSTOMER_MAX_CAP; i++) {
			try {
				custObjArray[i].join();
				System.out.println("Joined customer " + i);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		// join all the postal worker threads once processing of all customer
		// threads is done.
		for (int i = 0; i < POSTAL_WORKER_MAX_CAP; i++) {
			try {
				worker[i].join();
				System.out.println("Joined Postal Worker " + i);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}

	/**
	 * @param args
	 *            this method is responsible for starting the simulation.
	 */
	public static void main(String[] args) {
		(new PostOfficeSimulator()).startPostalOffice();
	}

	/**
	 * @author Rahul This class is responsible for requesting tasks to
	 *         postalWorker threads. This class is responsible for sharing
	 *         global variables and local variable values.
	 *
	 */
	public class Customer extends Thread {

		// customer ID and customer action task
		int custNumber;
		int custAction;

		public Customer(int i, int nextInt) {
			this.custAction = nextInt;
			this.custNumber = i;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Thread#run()
		 */
		public void run() {
			try {

				// local variables
				int custNum;
				int custActionNum;
				int postalWorkerNum;

				// semaphore to allow only 10 customers
				customerEntry.acquire();

				System.out.println("Customer " + custNumber
						+ " enters post office");

				// this is to save the customer ID to global variable, only one
				// thread should go through this code.
				mutex1.acquire();
				// critical section - start
				customerID = this.custNumber;
				custNum = this.custNumber;
				custActionID = this.custAction;
				custActionNum = this.custAction;
				mutex2.release();
				mutex3.acquire();
				postalWorkerNum = postalWorkerID;
				// critical section - end
				mutex4.release();

				// this will wait till postal worker is assigned to customer.
				// Once assigned, postalWorker thread signal to this semaphore
				// making its value one which allows thread to go ahead.
				postalWorkerAssigned.acquire();
				// this is responsible to request task to postal worker
				customerRequestTask(custNum, custActionNum, postalWorkerNum);
				// signal postal worker thread that customer thread requested
				// the task.
				customerRequested.release();
				// waiting till postal worker finishes the task.
				finished[custNum].acquire();
				// once finished notify customer that task is completed.
				customerTaskCompletionNotify(custNum, custActionNum);
				// release the customer thread, allow a new one
				customerEntry.release();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}

		/**
		 * @param customerNum
		 * @param custAction
		 * 
		 *            This function is responsible for notifying customer that
		 *            task is completed.
		 */
		private void customerTaskCompletionNotify(int customerNum,
				int custAction) {
			switch (custAction) {
			case 0:
				System.out.println("Customer" + customerNum
						+ " finished buying stamps");
				break;
			case 1:
				System.out.println("Customer" + customerNum
						+ " finished mailing a letter");
				break;
			case 2:
				System.out.println("Customer" + customerNum
						+ " finished mailing a package");
				break;

			default:
				break;
			}

		}

		/**
		 * @param customerNum
		 * @param taskNum
		 * @param postalWorkerNum
		 * @throws InterruptedException
		 * 
		 *             This method is responsible for requesting the tasks to
		 *             postal worker.
		 */
		private void customerRequestTask(int customerNum, int taskNum,
				int postalWorkerNum) throws InterruptedException {
			switch (taskNum) {
			case 0:
				System.out.println("Customer" + customerNum
						+ " asks postal worker" + postalWorkerNum
						+ " to buy stamps");
				break;
			case 1:
				System.out.println("Customer" + customerNum
						+ " asks postal worker" + postalWorkerNum
						+ " to mail a letter");
				break;
			case 2:
				System.out.println("Customer" + customerNum
						+ " asks postal worker" + postalWorkerNum
						+ " to mail a package");
				break;
			default:
				break;
			}
		}

	}

	/**
	 * @author Rahul
	 * 
	 *         This class is responsible for simulating the postal worker. It is
	 *         responsible for completing the requested tasks.
	 *
	 */
	public class PostalWorker extends Thread {

		// global variables
		int postalNumber;
		boolean work = true;
		
		public PostalWorker(int i) {
			this.postalNumber = i;
		}

		public void run() {

			while (work) {

				try {
					// local variables
					int custID;
					int postalWrkrID;
					int custActionNum;
					int count;

					// this is to allow only 3 postal workers at a time.
					postalWorker.acquire();
					// this is to retrieve the customer ID from global variable,
					// only one thread should go through this code.
					mutex2.acquire();
					// critical section - start
					custID = customerID;
					custActionNum = custActionID;
					mutex4.acquire();
					postalWrkrID = postalNumber;
					postalWorkerID = postalWrkrID;
					customerThreadCount--;
					count = customerThreadCount;
					// critical section - end
					mutex3.release();
					mutex1.release();

					System.out.println("Postal worker" + postalWrkrID
							+ " serving customer" + custID);

					// signal the customer thread to go ahead. as postal worker
					// is assigned to customer.
					postalWorkerAssigned.release();
					// wait for customer to request a task
					customerRequested.acquire();

					// work on the customer task
					workOnCustomerTask(custID, custActionNum, postalWrkrID);

					System.out.println("Postal worker" + postalWrkrID
							+ " finished serving customer" + custID);
					finished[custID].release();
					// release the postalWorker.
					
					if(count < 3){
						work = false;
					}
					postalWorker.release();

				} catch (InterruptedException e) {
					// TODO: handle exception
				}

			}

		}

		/**
		 * @param customerNum
		 * @param taskNum
		 * @param postalWorkerNo
		 * @throws InterruptedException
		 * 
		 *             This is responsible for implementing the logic for all
		 *             three tasks. For mailing a package scale resource is used
		 *             which can be used by only one postal worker at a time.
		 *             this is implemenetd through Semaphore.
		 */
		private void workOnCustomerTask(int customerNum, int taskNum,
				int postalWorkerNo) throws InterruptedException {
			switch (taskNum) {
			case 0:
				Thread.sleep(1000);
				break;
			case 1:
				Thread.sleep(1500);
				break;
			case 2:
				scale.acquire();
				// critical section - start
				System.out.println("Scales in use by postal worker"
						+ postalWorkerNo);
				Thread.sleep(2000);
				System.out.println("Scales released by postal worker"
						+ postalWorkerNo);
				// critical section - end
				scale.release();
				break;
			default:
				break;
			}

		}
	}

}
