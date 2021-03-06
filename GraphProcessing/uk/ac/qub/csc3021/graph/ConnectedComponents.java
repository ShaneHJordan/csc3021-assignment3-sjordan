package uk.ac.qub.csc3021.graph;

// Calculate the strongly connected components by propagating labels.
// This algorithm only works correctly for undirected graphs
public class ConnectedComponents {
    public static int[] compute(SparseMatrix matrix) {
        long tm_start = System.nanoTime();

        final int n = matrix.getNumVertices();
        int[] x = new int[n];
        int[] y = new int[n];
        final boolean verbose = true;
        final int max_iter = 100;
        int iter = 0;
        boolean change = true;

        for (int i = 0; i < n; ++i) {
            x[i] = y[i] = i; // Each vertex is assigned a unique label
        }

        CCRelax CCrelax = new CCRelax(x, y);

        double tm_init = (double) (System.nanoTime() - tm_start) * 1e-9;
        System.err.println("Initialisation: " + tm_init + " seconds");
        tm_start = System.nanoTime();

        ParallelContext context = ParallelContextHolder.get();

        double totalIterateTime = 0.0d;

        while (iter < max_iter && change) {
            // 1. Assign same label to connected vertices
            long startIterate = System.nanoTime();
            context.iterate(matrix, CCrelax);
            double timeInIterate = (double) (System.nanoTime() - startIterate) * 1e-9;
            System.err.println("Time in iterate: " + timeInIterate + " seconds");
            totalIterateTime += timeInIterate;
            // 2. Check changes and copy data over for new pass
            change = false;
            for (int i = 0; i < n; ++i) {
                if (x[i] != y[i]) {
                    x[i] = y[i];
                    change = true;
                }
            }

            double tm_step = (double) (System.nanoTime() - tm_start) * 1e-9;
            if (verbose)
                System.err.println("iteration " + iter
                        + " time=" + tm_step + " seconds");
            tm_start = System.nanoTime();
            ++iter;
        }

        System.err.println("Total time in iterate: " + totalIterateTime + " seconds.");

        // Post-process the labels

        // 1. Count number of components
        //    and map component IDs to narrow domain
        int ncc = 0;
        int[] remap = new int[n];
        for (int i = 0; i < n; ++i)
            if (x[i] == i)
                remap[i] = ncc++;

        if (verbose)
            System.err.println("Number of components: " + ncc);

        // 2. Calculate size of each component
        int[] sizes = new int[ncc];
        for (int i = 0; i < n; ++i)
            ++sizes[remap[x[i]]];

        if (verbose)
            System.err.println("ConnectedComponents: " + ncc + " components");

        return sizes;
    }

	private static class CCRelax implements Relax {
        int[] x;
        int[] y;

        CCRelax(int[] x_, int[] y_) {
            x = x_;
            y = y_;
        }

        public void relax(int src, int dst) {
            y[dst] = Math.min(y[dst], x[src]);
        }
    }
}
