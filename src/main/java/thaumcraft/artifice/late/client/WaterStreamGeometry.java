package thaumcraft.artifice.late.client;

/** The released CoreGLE eight-sided ANGLE/CAP/CLOSED contour, without an OpenGL dependency. */
final class WaterStreamGeometry {
    private static final double TOLERANCE=2.0E-6;
    @FunctionalInterface interface Sink { void vertex(double x,double y,double z,double u,double v,int index); }
    private WaterStreamGeometry() {}

    static void emit(double[][] points,double[] radii,Sink sink) {
        int count=points.length;
        if(count<3)return;
        int firstEnd=next(0,points);
        double[] firstDirection=subtract(points[firstEnd],points[firstEnd-1]);
        double[] up=firstDirection[0]==0.0&&firstDirection[2]==0.0?new double[]{1,1,1}:new double[]{0,1,0};
        double[] initial=subtract(points[1],points[0]);
        double initialLength=length(initial);
        if(initialLength==0.0)for(int i=1;i<count-2;i++) {
            initial=subtract(points[i+1],points[i]);initialLength=length(initial);
            if(initialLength!=0.0)break;
        }
        initial=scale(1.0/initialLength,initial);
        up=perpendicular(up,initial);
        if(length(up)==0.0)up=initial.clone();

        double[][] contour=new double[8][2];contour[0][0]=1.0;
        double sine=Math.sin(2.0*Math.PI/8),cosine=Math.cos(2.0*Math.PI/8);
        for(int j=1;j<8;j++) {
            contour[j][0]=contour[j-1][0]*cosine-contour[j-1][1]*sine;
            contour[j][1]=contour[j-1][0]*sine+contour[j-1][1]*cosine;
        }
        int current=1,end=next(1,points),progress=0;
        // FIND_NON_DEGENERATE_POINT returns the last tested adjacent difference, even when it skipped points.
        double segmentLength=length(subtract(points[end],points[end-1]));
        double[] frontPlane=bisector(points[0],points[1],points[end]);
        up=reflect(up,frontPlane);
        boolean first=true;
        while(end<count-1) {
            int following=next(end,points);
            double followingLength=length(subtract(points[following],points[following-1]));
            double[] backPlane=bisector(points[current],points[end],points[following]);
            double[][] frame=frame(subtract(points[end],points[current]),up);
            double[] frontNormal=toLocal(frame,frontPlane),backNormal=toLocal(frame,backPlane);
            double[][] front=new double[8][],back=new double[8][];
            for(int j=0;j<8;j++) {
                double[] a={radii[end-1]*contour[j][0],radii[end-1]*contour[j][1],0};
                double[] b={a[0],a[1],-segmentLength};
                front[j]=toWorld(frame,points[current],intersect(new double[3],frontNormal,a,b));
                a=new double[]{radii[end]*contour[j][0],radii[end]*contour[j][1],0};
                b=new double[]{a[0],a[1],-segmentLength};
                back[j]=toWorld(frame,points[current],intersect(new double[]{0,0,-segmentLength},backNormal,a,b));
            }
            if(first) { cap(front,false,end-1,sink);first=false; }
            if(end==count-2)cap(back,true,end,sink);
            // Preserve the original strip diagonal even for nonplanar miter faces; each triangle uses a degenerate quad.
            // Texture V advances one full repeat per segment.
            for(int j=0;j<8;j++) {
                int followingSide=(j+1)%8;
                vertex(sink,front[j],j/8.0,progress,end-1);
                vertex(sink,back[j],j/8.0,progress+1,end);
                vertex(sink,front[followingSide],(j+1)/8.0,progress,end-1);
                vertex(sink,front[followingSide],(j+1)/8.0,progress,end-1);
                vertex(sink,front[followingSide],(j+1)/8.0,progress,end-1);
                vertex(sink,back[j],j/8.0,progress+1,end);
                vertex(sink,back[followingSide],(j+1)/8.0,progress+1,end);
                vertex(sink,back[followingSide],(j+1)/8.0,progress+1,end);
            }
            progress++;
            segmentLength=followingLength;current=end;end=following;
            frontPlane=backPlane;up=reflect(up,frontPlane);
        }
    }

    private static void cap(double[][] ring,boolean reverse,int index,Sink sink) {
        // CoreGLE's GLU cap is an unculled convex octagon and emits no texture coordinates.
        // Integer side endpoints repeat the same seam texel; use it explicitly in the modern vertex API.
        for(int j=1;j<7;j++) {
            int a=reverse?7:0,b=reverse?7-j:j,c=reverse?6-j:j+1;
            vertex(sink,ring[a],0,0,index);vertex(sink,ring[b],0,0,index);
            vertex(sink,ring[c],0,0,index);vertex(sink,ring[c],0,0,index);
        }
    }
    private static void vertex(Sink sink,double[] p,double u,double v,int index) { sink.vertex(p[0],p[1],p[2],u,v,index); }
    private static int next(int index,double[][] points) {
        int i=index;double distance,threshold;
        do {
            distance=length(subtract(points[i+1],points[i]));
            threshold=length(new double[]{points[i+1][0]+points[i][0],points[i+1][1]+points[i][1],points[i+1][2]+points[i][2]})*TOLERANCE;
            i++;
        }while(distance<=threshold&&i<points.length-1);
        return i;
    }
    private static double[] bisector(double[] a,double[] b,double[] c) {
        double[] before=subtract(b,a),after=subtract(c,b);
        double beforeLength=length(before),afterLength=length(after);
        if(beforeLength<=TOLERANCE*afterLength)return afterLength==0.0?new double[3]:scale(1.0/afterLength,after);
        if(afterLength<=TOLERANCE*beforeLength)return scale(1.0/beforeLength,before);
        before=scale(1.0/beforeLength,before);after=scale(1.0/afterLength,after);
        double dot=dot(after,before);
        if(dot>=.999998||dot<=-.999998)return before;
        return normalize(new double[]{dot*(after[0]+before[0])-after[0]-before[0],
                dot*(after[1]+before[1])-after[1]-before[1],dot*(after[2]+before[2])-after[2]-before[2]});
    }
    private static double[] intersect(double[] p,double[] normal,double[] a,double[] b) {
        double denominator=(a[0]-b[0])*normal[0];
        denominator+=(a[1]-b[1])*normal[1];denominator+=(a[2]-b[2])*normal[2];
        if(denominator==0.0)return new double[3];
        double numerator=(p[0]-b[0])*normal[0];
        numerator+=(p[1]-b[1])*normal[1];numerator+=(p[2]-b[2])*normal[2];
        double t=numerator/denominator,other=1.0-t;
        return new double[]{t*a[0]+other*b[0],t*a[1]+other*b[1],t*a[2]+other*b[2]};
    }
    private static double[][] frame(double[] direction,double[] up) {
        double[] z=scale(-1,normalize(direction)),y=normalize(perpendicular(up,z));
        return new double[][]{cross(y,z),y,z};
    }
    private static double[] toLocal(double[][] frame,double[] p) { return new double[]{dot(frame[0],p),dot(frame[1],p),dot(frame[2],p)}; }
    private static double[] toWorld(double[][] frame,double[] origin,double[] p) {
        return new double[]{frame[0][0]*p[0]+frame[1][0]*p[1]+frame[2][0]*p[2]+origin[0],
                frame[0][1]*p[0]+frame[1][1]*p[1]+frame[2][1]*p[2]+origin[1],
                frame[0][2]*p[0]+frame[1][2]*p[1]+frame[2][2]*p[2]+origin[2]};
    }
    private static double[] subtract(double[] a,double[] b) { return new double[]{a[0]-b[0],a[1]-b[1],a[2]-b[2]}; }
    private static double[] scale(double scale,double[] a) { return new double[]{scale*a[0],scale*a[1],scale*a[2]}; }
    private static double dot(double[] a,double[] b) { return a[0]*b[0]+a[1]*b[1]+a[2]*b[2]; }
    private static double length(double[] a) { return Math.sqrt(dot(a,a)); }
    private static double[] normalize(double[] a) { double length=length(a);return length==0.0?new double[3]:scale(1.0/length,a); }
    private static double[] perpendicular(double[] a,double[] normal) { double dot=dot(a,normal);return new double[]{a[0]-dot*normal[0],a[1]-dot*normal[1],a[2]-dot*normal[2]}; }
    private static double[] reflect(double[] a,double[] normal) { double dot=dot(a,normal);return new double[]{a[0]-2.0*dot*normal[0],a[1]-2.0*dot*normal[1],a[2]-2.0*dot*normal[2]}; }
    private static double[] cross(double[] a,double[] b) { return new double[]{a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]}; }
}
