package thaumcraft.client.theory;

import java.util.Arrays;
import java.util.List;
import thaumcraft.research.theory.TheoryCard;
import thaumcraft.research.theory.TheorySession;

/** Released GuiResearchTable cardZoomOut/cardZoomIn/cardHover motion, detached from server payment. */
final class TheoryCardAnimation {
    enum Phase { NONE, DEAL, OFFER, RESOLVE }
    private List<TheoryCard> cards=List.of();
    private String offered="";
    private Phase phase=Phase.NONE;
    private final float[] hover=new float[3],out=new float[3],in=new float[3];
    private int selected=-1,previousPlaced,pageFlips;
    void sync(TheorySession session) {
        if(session==null){clear();previousPlaced=0;return;}
        String choices=session.choices().stream().map(c->c.save().toString()).reduce("",(a,b)->a+b);
        if(!choices.isEmpty()&&!choices.equals(offered)) {
            cards=List.copyOf(session.choices());offered=choices;phase=Phase.DEAL;selected=-1;
            Arrays.fill(hover,0);Arrays.fill(out,0);Arrays.fill(in,0);
        } else if(choices.isEmpty()&&!cards.isEmpty()&&phase!=Phase.RESOLVE) {
            // A changed authoritative paid lastCard alone starts cosmetic resolution.
            selected=-1;
            TheoryCard last=session.lastCard();
            if(last!=null&&session.placedCards()>previousPlaced)for(int i=0;i<cards.size();i++)if(cards.get(i).save().equals(last.save())){selected=i;break;}
            if(selected>=0)phase=Phase.RESOLVE;else clear();
        }
        previousPlaced=session.placedCards();
    }
    void advance(float partialTicks,float x,float y) {
        // BETA26 advances on rendered frames using the passed render fraction, not wall time.
        boolean dealt=true;
        for(int i=0;i<cards.size();i++) {
            int left=133-55*cards.size()+110*i;
            if(out[i]>=.95F&&phase!=Phase.RESOLVE) {
                if(x>=left&&x<left+100&&y>=40&&y<160)hover[i]+=Math.max((.25F-hover[i])/3F*partialTicks,.0025F);
                else hover[i]-=.1F*partialTicks;
            }
            if(i==cards.size()-1||out[i+1]>.6F) {
                float previous=out[i];
                out[i]+=Math.max((1-out[i])/5F*partialTicks,.0025F);
                if(previous==0&&out[i]>0)pageFlips++;
            }
            if(phase==Phase.RESOLVE) {
                in[i]+=i==selected?(float)Math.max((1-in[i])/3F*partialTicks,.0025):.3F*partialTicks;
                hover[i]=1-in[i];
            }
            out[i]=net.minecraft.util.Mth.clamp(out[i],0,1);in[i]=net.minecraft.util.Mth.clamp(in[i],0,1);hover[i]=net.minecraft.util.Mth.clamp(hover[i],0,.25F);
            dealt&=out[i]>=1;
        }
        if(phase==Phase.DEAL&&dealt)phase=Phase.OFFER;
        if(phase==Phase.RESOLVE&&selected>=0&&in[selected]>=1)clear();
    }
    private void clear(){cards=List.of();offered="";phase=Phase.NONE;selected=-1;Arrays.fill(hover,0);Arrays.fill(out,0);Arrays.fill(in,0);}
    Phase phase(){return phase;}
    List<TheoryCard> cards(){return cards;}
    boolean busy(){return phase==Phase.DEAL||phase==Phase.RESOLVE;}
    boolean selected(int index){return index==selected;}
    float hover(int index){return hover[index];}
    float deal(int index){return out[index];}
    float zoomIn(int index){return in[index];}
    float resolution(){return selected>=0?in[selected]:0;}
    int takePageFlips(){int count=pageFlips;pageFlips=0;return count;}
}
