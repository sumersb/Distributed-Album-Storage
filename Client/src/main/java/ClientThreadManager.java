import java.io.FileNotFoundException;
import java.util.concurrent.LinkedBlockingQueue;
import com.tdunning.math.stats.MergingDigest;
public class ClientThreadManager {

    private static final float MILLISECOND_TO_SECOND= (float) 1 /1000;

    private static final long SECOND_TO_MILLISECOND = 1000;

    private static final int STARTUP_THREAD_COUNT = 10;

    private static  final int ITERATIONS_PER_STARTUP_THREAD = 100;

    private static final int ITERATIONS_PER_THREAD = 1000 ;

    private int threadGroupSize;

    private int numThreadGroups;

    private int delay;

    private String IPAddr;


    public ClientThreadManager(int threadGroupSize, int numThreadGroups, int delay, String IPAddr) {
        this.threadGroupSize = threadGroupSize;
        this.numThreadGroups = numThreadGroups;
        this.delay = delay;
        this.IPAddr = IPAddr;
    }

    public void callThreads() throws InterruptedException, FileNotFoundException {

        //Create threadsafe queue to store latency information
        LinkedBlockingQueue<CallInfo> callInfoQueue= new LinkedBlockingQueue<>();
        //Creates writer instance to process callInfo information of all threads
        Writer writer = new Writer(callInfoQueue);
        writer.start();


        // Initialize jagged threadArray with 10 Threads in First Index and threadGroupSize threads in rest of indexes
        Thread[][] threadArray = new Thread[numThreadGroups + 1][];
        threadArray[0] = new Thread[STARTUP_THREAD_COUNT];
        for (int i = 1; i <= numThreadGroups;i++) {
            threadArray[i] = new Thread[threadGroupSize];
        }
        // Startup threads initiated
        for (int i = 0; i < threadArray[0].length ; i++) {
            threadArray[0][i] = new Client(IPAddr,ITERATIONS_PER_STARTUP_THREAD, callInfoQueue, false);
            threadArray[0][i].start();
        }
        //Wait for completion of startup threads
        for (int i = 0; i < threadArray[0].length ; i++) {
            threadArray[0][i].join();
        }
        //Take startTime before test threads are run
        long startTime = System.currentTimeMillis();

        //Iterate through threadGroups and start threads with delay in between
        for (int i = 1; i < threadArray.length ; i++) {
            for (int j = 0; j < threadArray[i].length; j++) {
                threadArray[i][j] = new Client(IPAddr, ITERATIONS_PER_THREAD,callInfoQueue, true);
                threadArray[i][j].start();
            }
            //Delay between next thread group iteration
            Thread.sleep( delay * SECOND_TO_MILLISECOND);
        }

        //Wait for completion of all threads
        for (int i = 1; i < threadArray.length ; i++) {
            for (int j = 0; j < threadArray[i].length; j++) {
                threadArray[i][j].join();
            }
        }
        //Take endTime time now threads have finished
        long endTime = System.currentTimeMillis();

        //Add marker to callInfoQueue to tell writer to stop
        callInfoQueue.add(new CallInfo(-1,null,-1,-1));

        //Pass information stores to printStats
        printStats(startTime, endTime, writer);
    }


    private void printStats(long startTime, long endTime, Writer writer) {

        //ITERATIONS_PER_THREAD multiplied BY 2 to account for get and post request
        int callSize = numThreadGroups * threadGroupSize * ITERATIONS_PER_THREAD * 2;

        //Self-explanatory
        float wallTime = (endTime - startTime) * MILLISECOND_TO_SECOND;

        float throughput = (float) (writer.getGetSuccess()+writer.getPostSuccess()) / wallTime;

        float getMean = (float) writer.getGetLatencySum() / writer.getGetSuccess();

        float postMean = (float) writer.getPostLatencySum() / writer.getPostSuccess();

        double getMedian = writer.getGetDigest().quantile(0.5);

        double getPercentile99 = writer.getGetDigest().quantile(0.99);

        double postMedian = writer.getPostDigest().quantile(0.5);

        double postPercentile99 = writer.getPostDigest().quantile(0.99);

        //Print out all information to terminal
        System.out.println();
        System.out.println("Number Thread Groups: " + numThreadGroups);
        System.out.println("Thread Group Size: " + threadGroupSize);

        System.out.println();
        System.out.println("Server Calls: " + callSize);
        System.out.println("Success: "+ (writer.getGetSuccess()+writer.getPostSuccess()));
        System.out.println("Failures: "+(callSize-(writer.getGetSuccess()+writer.getPostSuccess())));

        System.out.println();
        System.out.println("Wall Time: " + wallTime + " seconds");
        System.out.println("Throughput: " + throughput + " calls per second");

        System.out.println();
        System.out.println("Min Get Latency: " + writer.getMinGetLatency());
        System.out.println("Get Mean: " + getMean);
        System.out.println("Get Median: " + getMedian);
        System.out.println("Get 99 Percentile: " + getPercentile99);
        System.out.println("Get Max Latency: " + writer.getMaxGetLatency());

        System.out.println();
        System.out.println("Min Post Latency: " + writer.getMinPostLatency());
        System.out.println("Post Mean: " + postMean);
        System.out.println("Post Median: " + postMedian);
        System.out.println("Post 99 Percentile: " + postPercentile99);
        System.out.println("Post Max Latency: " + writer.getMaxPostLatency());


    }


    public int getThreadGroupSize() {
        return threadGroupSize;
    }

    public void setThreadGroupSize(int threadGroupSize) {
        this.threadGroupSize = threadGroupSize;
    }

    public int getNumThreadGroups() {
        return numThreadGroups;
    }

    public void setNumThreadGroups(int numThreadGroups) {
        this.numThreadGroups = numThreadGroups;
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public String getIPAddr() {
        return IPAddr;
    }

    public void setIPAddr(String IPAddr) {
        this.IPAddr = IPAddr;
    }

}
