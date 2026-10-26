package thaumcraft.research.theory;

import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.util.RandomSource;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import thaumcraft.research.KnowledgeStore;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class TheoryVisualPersistenceGameTests {
    private static CompoundTag card(long seed) {
        CompoundTag tag=new CompoundTag();tag.putString("Id","study");tag.putLong("Seed",seed);tag.putBoolean("FromAid",true);tag.putString("Category","BASICS");tag.putInt("Amount",0);return tag;
    }
    @GameTest(template="empty")
    public static void paidSelectionArchivesOnlyPreviousCardAndSurvivesReload(GameTestHelper h) {
        var player=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"theory_visual"));
        var tag=TheorySession.create(player.getUUID(),KnowledgeStore.get(player),Set.of(),RandomSource.create(41)).save();
        tag.putInt("PlacedCards",1);tag.putInt("Inspiration",4);tag.put("LastCard",card(777));
        ListTag choices=new ListTag();choices.add(card(888));tag.put("Choices",choices);
        var session=TheorySession.load(tag);h.assertTrue(session!=null,"Initialized session lost");
        var before=session.save();
        h.assertTrue(!session.select(player,4)&&session.save().equals(before),"Rejected selection changed cosmetic history");
        h.assertTrue(session.select(player,0)&&session.inspiration()==3&&session.placedCards()==2,"Actual accepted selection/payment failed");
        h.assertTrue(session.savedCardSeeds().equals(List.of(777L))&&session.lastCard().seed()==888,"History must contain previous card, not new last twice");
        var reloaded=TheorySession.load(session.save());
        h.assertTrue(reloaded!=null&&reloaded.save().equals(session.save()),"Cosmetic stack changed during native NBT roundtrip");h.succeed();
    }
    @GameTest(template="empty")
    public static void oldSavedSessionKeepsRealLastCardWithoutInventingHistory(GameTestHelper h) {
        var player=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"theory_old_visual"));
        var tag=TheorySession.create(player.getUUID(),KnowledgeStore.get(player),Set.of(),RandomSource.create(41)).save();
        tag.putInt("PlacedCards",7);tag.put("LastCard",card(1234));tag.remove("SavedCards");
        var session=TheorySession.load(tag);
        h.assertTrue(session!=null&&session.placedCards()==7&&session.savedCardSeeds().isEmpty()&&session.lastCard().seed()==1234,"Old actual session must load without fabricated historical seeds");
        var again=TheorySession.load(session.save());h.assertTrue(again!=null&&again.save().equals(session.save()),"Old-save visual upgrade is not stable");h.succeed();
    }
}
