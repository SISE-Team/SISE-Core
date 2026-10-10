package local.sise.core.mixin;
import java.util.*;
import java.util.zip.*;
import local.sise.core.performance.ZipDirectoryIndex;
import net.minecraft.server.packs.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(value=FilePackResources.class,remap=false)
public class FilePackIndexMixin {
 @Shadow private String addPrefix(String path){throw new AssertionError();}
 @Redirect(method="listResources",at=@At(value="INVOKE",target="Ljava/util/zip/ZipFile;entries()Ljava/util/Enumeration;"))
 private Enumeration<? extends ZipEntry> sise$list(ZipFile zip,PackType type,String namespace,String path,PackResources.ResourceOutput output) {
  return ZipDirectoryIndex.entries(zip,addPrefix(type.getDirectory()+"/"+namespace+"/")+path+"/");
 }
}
