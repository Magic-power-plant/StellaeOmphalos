package com.mpp.stellaeomphalos.client.sign;

import com.mpp.stellaeomphalos.constellation.sign.Sign;
import com.mpp.stellaeomphalos.constellation.sign.SignDiscoveryView;
import com.mpp.stellaeomphalos.constellation.starmap.StarPoint;
import com.mpp.stellaeomphalos.constellation.starmap.StarLine;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Immutable presentation-only definitions from the server. Never grant discovery authority. */
public final class SignDefinitionMirror {
    private record VisualSign(ResourceLocation id,int renderColor,List<StarPoint> stars,List<StarLine> lines,boolean major) implements Sign {
        public Component displayName(){return Component.translatable("sign."+id.getNamespace()+"."+id.getPath());}
        public List<ItemStack> signatureItems(){return List.of();}
        public boolean canDiscover(ServerPlayer player,SignDiscoveryView view){return false;}
    }
    private static Map<ResourceLocation,Sign> byId=Map.of();
    private static Map<Integer,Sign> byNumber=Map.of();
    private static long revision;
    private SignDefinitionMirror(){}
    public static long revision(){return revision;}
    public static void replace(CompoundTag data){
        var ids=new HashMap<ResourceLocation,Sign>();var numbers=new HashMap<Integer,Sign>();
        int count=0;
        for(String key:data.getAllKeys()){
            if(++count>512)break;
            var id=ResourceLocation.tryParse(key);if(id==null)continue;
            var entry=data.getCompound(key);var points=new ArrayList<StarPoint>();var lines=new ArrayList<StarLine>();
            var stars=entry.getList("Stars",Tag.TAG_COMPOUND);var edges=entry.getList("Edges",Tag.TAG_COMPOUND);
            if(stars.size()>128||edges.size()>256)continue;
            try{
                for(int i=0;i<stars.size();i++){var p=stars.getCompound(i);points.add(new StarPoint(p.getInt("X"),p.getInt("Y")));}
                var available=new HashSet<>(points);
                for(int i=0;i<edges.size();i++){var e=edges.getCompound(i);var a=new StarPoint(e.getInt("AX"),e.getInt("AY"));var b=new StarPoint(e.getInt("BX"),e.getInt("BY"));
                    if(!available.contains(a)||!available.contains(b))throw new IllegalArgumentException("Dangling edge");lines.add(new StarLine(a,b));}
                var sign=new VisualSign(id,entry.getInt("Color"),List.copyOf(points),List.copyOf(lines),entry.getBoolean("Major"));
                ids.put(id,sign);numbers.put(entry.getInt("Number"),sign);
            }catch(IllegalArgumentException invalid){com.mojang.logging.LogUtils.getLogger().warn("Invalid synchronized star geometry {}",key);}
        }
        byId=Map.copyOf(ids);byNumber=Map.copyOf(numbers);revision++;
    }
    public static Sign byId(ResourceLocation id){return byId.getOrDefault(id,com.mpp.stellaeomphalos.constellation.sign.SignRegistry.byId(id));}
    public static Sign byNumber(int id){return byNumber.getOrDefault(id,com.mpp.stellaeomphalos.constellation.sign.SignRegistry.byNumericId(id));}
    public static boolean major(Sign sign){return sign instanceof VisualSign visual?visual.major():sign instanceof com.mpp.stellaeomphalos.constellation.sign.MajorSign;}
    public static void clear(){byId=Map.of();byNumber=Map.of();revision++;}
}
