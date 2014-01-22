import net.sf.javailp.*;

import java.io.PrintWriter;

/**
 * Created by gomes on 1/21/14.
 */
public final class LP_solution {

    protected int I;
    protected int K;
    protected SegmentSolution [] Xopt;

    ///////////////////////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////////////////////

    public LP_solution(Result result,FwyNetwork fwy,int K){

        this.I = fwy.segments.size();
        this.K = K;
        this.Xopt = new SegmentSolution[I];

        int i,k;
        for(i=0;i<I;i++){

            FwySegment seg = fwy.segments.get(i);

            Xopt[i] = new SegmentSolution(seg,K);

            Xopt[i].n[0] = seg.no;
            for(k=0;k<K;k++){
                Xopt[i].n[k+1] = result.get(getVar("n",i,k+1)).doubleValue();
                Xopt[i].f[k] = result.get(getVar("f", i, k)).doubleValue();
            }

            if(seg.is_metered){
                Xopt[i].l[0] = seg.lo;
                for(k=0;k<K;k++){
                    Xopt[i].l[k+1] = result.get(getVar("l",i,k+1)).doubleValue();
                    Xopt[i].r[k] = result.get(getVar("r", i, k)).doubleValue();
                }
            }

        }

    }

    ///////////////////////////////////////////////////////////////////
    // class
    ///////////////////////////////////////////////////////////////////

    public class SegmentSolution {
        protected double [] n;
        protected double [] l;
        protected double [] f;
        protected double [] r;

        public SegmentSolution(FwySegment fseg,int K){
            n = new double[K+1];
            f = new double[K];
            if(fseg.is_metered){
                l = new double[K+1];
                r = new double[K];
            }
        }
        public double [] get(String name){
            if(name.compareTo("n")==0)
                return n;
            if(name.compareTo("l")==0)
                return l;
            if(name.compareTo("r")==0)
                return r;
            if(name.compareTo("f")==0)
                return f;
            return null;
        }

    }

    ///////////////////////////////////////////////////////////////////
    // private
    ///////////////////////////////////////////////////////////////////

    private static String getVar(String name,int seg_index,int timestep){
        return LP_ramp_metering.getVar(name,seg_index,timestep);
    }

    ///////////////////////////////////////////////////////////////////
    // print
    ///////////////////////////////////////////////////////////////////

    public String print(String var,int seg_index,boolean matlab){
        String str = "";
        int lastK;
        if(var.compareTo("n")==0 || var.compareTo("l")==0)
            lastK = K+1;
        else
            lastK = K;
        double [] x = Xopt[seg_index].get(var);
        if(x!=null)
            for(int k=0;k<lastK;k++){
                if(matlab)
                    str = str.concat(String.format("%s(%d,%d)=%.2f;\n",var,seg_index+1,k+1,x[k]));
                else
                    str = str.concat(String.format("%s[%d][%d]=%.2f\n",var,seg_index,k,x[k]));
            }
        return str;
    }

    public String print(String var,int seg_index){
        return print(var,seg_index,false);
    }

    public String print(String var,boolean matlab){
        String str = "";
        if(matlab)
            str = String.format("%s=nan(%d,%d);\n",var,I,K+1);
        for(int i=0;i<Xopt.length;i++)
            str = str.concat(print(var,i,matlab));
        return str;
    }

    public String print(String var){
        return print(var,false);
    }

    public String print(boolean matlab){
        return print("n",matlab)+"\n"+ print("f",matlab)+"\n"+ print("l",matlab)+"\n"+ print("r",matlab);
    }

    public String print(){
        return print(false);
    }

    public void print_to_matlab(String function_name) throws Exception {
        PrintWriter pw = new PrintWriter("out\\" + function_name + ".m");
        pw.print("function [n,l,f,r]=" + function_name + "()\n");
        pw.print(print(true));
        pw.close();
    }

    @Override
    public String toString() {
        return print();
    }
}
