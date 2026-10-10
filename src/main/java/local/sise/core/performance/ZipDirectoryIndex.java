package local.sise.core.performance;
import java.util.*;
import java.util.zip.*;

/** A directory index belongs to one open ZIP, so reopening/replacing a pack cannot reuse it. */
public final class ZipDirectoryIndex {
 private static final Map<ZipFile,Map<String,List<ZipEntry>>> INDEXES=new WeakHashMap<>();
 public static Enumeration<? extends ZipEntry> entries(ZipFile zip,String directory) {
  if(Boolean.getBoolean("sise.reload.disableZipIndex"))return zip.entries();
  Map<String,List<ZipEntry>> index;
  synchronized(INDEXES){index=INDEXES.computeIfAbsent(zip,ZipDirectoryIndex::build);}
  return Collections.enumeration(index.getOrDefault(directory,List.of()));
 }
 private static Map<String,List<ZipEntry>> build(ZipFile zip) {
  var index=new HashMap<String,List<ZipEntry>>();var entries=zip.entries();
  while(entries.hasMoreElements()) {
   // Strip implementation-specific ZipFileEntry ownership to keep the weak key weak.
   ZipEntry entry=new ZipEntry(entries.nextElement());if(entry.isDirectory())continue;
   String name=entry.getName();
   for(int at=name.indexOf('/');at>=0;at=name.indexOf('/',at+1))
    index.computeIfAbsent(name.substring(0,at+1),ignored->new ArrayList<>()).add(entry);
  }
  index.replaceAll((key,list)->List.copyOf(list));return Map.copyOf(index);
 }
}
