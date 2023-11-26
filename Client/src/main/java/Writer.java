import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.FileWriter;
import java.util.concurrent.LinkedBlockingQueue;

import com.tdunning.math.stats.MergingDigest;




public class Writer extends Thread {



    private LinkedBlockingQueue<CallInfo> callInfoQueue;

    private int getSuccess = 0;

    private int postSuccess = 0;

    private long getLatencySum = 0;

    private long postLatencySum = 0;

    private long maxPostLatency = 0;

    private long minPostLatency = Long.MAX_VALUE;

    private long maxGetLatency = 0;

    private long minGetLatency = Long.MAX_VALUE;

    private final int MILLISECONDS_PER_SECOND = 1000;

    private MergingDigest getDigest = new MergingDigest(50,1000,100);
    private MergingDigest postDigest = new MergingDigest(50,1000,100);



    public Writer(LinkedBlockingQueue<CallInfo> callInfoQueue) {
        this.callInfoQueue = callInfoQueue;
    }

    public void run() {
        //Creates variables to track throughput every second
        long start = System.currentTimeMillis();
        long throughputEndTime = start;
        long callEndTime;
        int callCount = 0;
        int second=0;

//        CSVFormat csvFormat = CSVFormat.DEFAULT.withHeader("Start Time","Request Type","Latency","Response Code");
        try {
//            FileWriter fileWriter = new FileWriter("latency.csv");
//            CSVPrinter csvPrinter = new CSVPrinter(fileWriter,csvFormat);
            while (true) {
                CallInfo callInfo = callInfoQueue.take();
                long latency = callInfo.getLatency();
                if (callInfo.getResponseCode() == -1) {
                    break;
                } else {
//                    csvPrinter.printRecord(
//                            callInfo.getStartTime(),
//                            callInfo.getRequestType(),
//                            latency,
//                            callInfo.getResponseCode()
//                    );
                    if (callInfo.getResponseCode() == 200) {
                        getDigest.add(latency);
                        maxGetLatency = Math.max(maxGetLatency,latency);
                        minGetLatency = Math.min(minGetLatency,latency);
                        getSuccess += 1;
                        getLatencySum += latency;

                    } else if (callInfo.getResponseCode() == 201) {
                        postDigest.add(latency);
                        maxPostLatency = Math.max(maxPostLatency,latency);
                        minPostLatency = Math.min(minPostLatency,latency);
                        postSuccess += 1;
                        postLatencySum += latency;
                    }
                    callEndTime = callInfo.getStartTime() + latency;

                    //Calculates and prints throughput every second
                    if (System.currentTimeMillis()>=throughputEndTime) {
                        System.out.print("("+((throughputEndTime-start)/MILLISECONDS_PER_SECOND)+","+callCount+"),");
                        throughputEndTime+=MILLISECONDS_PER_SECOND;
                        callCount=1;
                        second+=1;
                    } else {
                        callCount+=1;
                    }
                }
            }
//            csvPrinter.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

         ;
    }

    public LinkedBlockingQueue<CallInfo> getCallInfoQueue() {
        return callInfoQueue;
    }

    public void setCallInfoQueue(LinkedBlockingQueue<CallInfo> callInfoQueue) {
        this.callInfoQueue = callInfoQueue;
    }

    public MergingDigest getGetDigest() {
        return getDigest;
    }

    public void setGetDigest(MergingDigest getDigest) {
        this.getDigest = getDigest;
    }

    public MergingDigest getPostDigest() {
        return postDigest;
    }

    public void setPostDigest(MergingDigest postDigest) {
        this.postDigest = postDigest;
    }

    public long getMaxPostLatency() {
        return maxPostLatency;
    }

    public void setMaxPostLatency(long maxPostLatency) {
        this.maxPostLatency = maxPostLatency;
    }

    public long getMinPostLatency() {
        return minPostLatency;
    }

    public void setMinPostLatency(long minPostLatency) {
        this.minPostLatency = minPostLatency;
    }

    public long getMaxGetLatency() {
        return maxGetLatency;
    }

    public void setMaxGetLatency(long maxGetLatency) {
        this.maxGetLatency = maxGetLatency;
    }

    public long getMinGetLatency() {
        return minGetLatency;
    }

    public void setMinGetLatency(long minGetLatency) {
        this.minGetLatency = minGetLatency;
    }

    public int getGetSuccess() {
        return getSuccess;
    }

    public void setGetSuccess(int getSuccess) {
        this.getSuccess = getSuccess;
    }

    public int getPostSuccess() {
        return postSuccess;
    }

    public void setPostSuccess(int postSuccess) {
        this.postSuccess = postSuccess;
    }

    public long getGetLatencySum() {
        return getLatencySum;
    }

    public void setGetLatencySum(long getLatencySum) {
        this.getLatencySum = getLatencySum;
    }

    public long getPostLatencySum() {
        return postLatencySum;
    }

    public void setPostLatencySum(long postLatencySum) {
        this.postLatencySum = postLatencySum;
    }

}
