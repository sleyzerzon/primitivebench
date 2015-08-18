package info.jerrinot.primitive;

import info.jerrinot.primitive.impl.ArrayIntList;
import info.jerrinot.primitive.impl.ByteBufferIntList;
import info.jerrinot.primitive.impl.UnsafeIntList;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.profile.LinuxPerfAsmProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Random;

@State(Scope.Benchmark)
public class IntListBenchmark {

    private static final int MAXIMUM_VALUE = 10;
    private static final int ITEM_COUNT = 10_000_000;

    @Param({"array", "bb", "directbb", "unsafe"})
    private String method;

    private IntList list1;
    private IntList list2;
    private Random r;

    private int valueToFind;


    @Setup(Level.Invocation)
    public void selectNextIndex() {
        valueToFind = r.nextInt(ITEM_COUNT);
    }

    @Setup
    public void setUp() {
        createLists();
        r = new Random();
        loadInitialData();
    }

    private void loadInitialData() {
        for (int i = 0; i < ITEM_COUNT; i++) {
            int value = r.nextInt(MAXIMUM_VALUE);
            list1.add(value);
            value = r.nextInt(MAXIMUM_VALUE);
            list2.add(value);
        }
    }

    private void createLists() {
        switch (method) {
            case "array" :
                list1 = new ArrayIntList(ITEM_COUNT);
                list2 = new ArrayIntList(ITEM_COUNT);
                break;
            case "bb":
                list1 = new ByteBufferIntList(ITEM_COUNT, false);
                list2 = new ByteBufferIntList(ITEM_COUNT, false);
                break;
            case "directbb":
                list1 = new ByteBufferIntList(ITEM_COUNT, true);
                list2 = new ByteBufferIntList(ITEM_COUNT, true);
                break;
            case "unsafe":
                list1 = new UnsafeIntList(ITEM_COUNT);
                list2 = new UnsafeIntList(ITEM_COUNT);
                break;
            default:
                throw new IllegalArgumentException("Unknown mode: " + method);
        }
    }

    @Benchmark
    public int fullScan_lazy() {
        int findCount = 0;
        for (int i = 0; i < list1.size(); i++) {
            if (list1.elementAt(i) == valueToFind || list2.elementAt(i) == valueToFind) {
                findCount++;
            }
        }
        return findCount;
    }

    @Benchmark
    public int fullScan_eager() {
        int findCount = 0;
        for (int i = 0; i < list1.size(); i++) {
            int value1 = list1.elementAt(i);
            int value2 = list2.elementAt(i);
            if (value1 == valueToFind || value2 == valueToFind) {
                findCount++;
            }
        }
        return findCount;
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(IntListBenchmark.class.getSimpleName())
                .warmupIterations(5)
                .measurementIterations(5)
                .threads(1)
                .forks(2)
                .addProfiler(LinuxPerfAsmProfiler.class)
                .build();

        new Runner(opt).run();
    }

}
