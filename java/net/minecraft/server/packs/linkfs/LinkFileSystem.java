package net.minecraft.server.packs.linkfs;

import com.google.common.base.Splitter;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchService;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;

public class LinkFileSystem extends FileSystem {
   private static final Set<String> VIEWS = Set.of("basic");
   public static final String PATH_SEPARATOR = "/";
   private static final Splitter PATH_SPLITTER = Splitter.on('/');
   private final FileStore store;
   private final FileSystemProvider provider = new LinkFSProvider();
   private final LinkFSPath root;

   LinkFileSystem(String pName, LinkFileSystem.DirectoryEntry pRoot) {
      this.store = new LinkFSFileStore(pName);
      this.root = buildPath(pRoot, this, "", null);
   }

   private static LinkFSPath buildPath(LinkFileSystem.DirectoryEntry pDirectory, LinkFileSystem pFileSystem, String pName, @Nullable LinkFSPath pParent) {
      Object2ObjectOpenHashMap<String, LinkFSPath> $$4 = new Object2ObjectOpenHashMap();
      LinkFSPath $$5 = new LinkFSPath(pFileSystem, pName, pParent, new PathContents.DirectoryContents($$4));
      pDirectory.files
         .forEach((p_249491_, p_250850_) -> $$4.put(p_249491_, new LinkFSPath(pFileSystem, p_249491_, $$5, new PathContents.FileContents(p_250850_))));
      pDirectory.children.forEach((p_251592_, p_251728_) -> $$4.put(p_251592_, buildPath(p_251728_, pFileSystem, p_251592_, $$5)));
      $$4.trim();
      return $$5;
   }

   @Override
   public FileSystemProvider provider() {
      return this.provider;
   }

   @Override
   public void close() {
   }

   @Override
   public boolean isOpen() {
      return true;
   }

   @Override
   public boolean isReadOnly() {
      return true;
   }

   @Override
   public String getSeparator() {
      return "/";
   }

   @Override
   public Iterable<Path> getRootDirectories() {
      return List.of(this.root);
   }

   @Override
   public Iterable<FileStore> getFileStores() {
      return List.of(this.store);
   }

   @Override
   public Set<String> supportedFileAttributeViews() {
      return VIEWS;
   }

   @Override
   public Path getPath(String pFirst, String... pMore) {
      Stream<String> $$2 = Stream.of(pFirst);
      if (pMore.length > 0) {
         $$2 = Stream.concat($$2, Stream.of(pMore));
      }

      String $$3 = $$2.collect(Collectors.joining("/"));
      if ($$3.equals("/")) {
         return this.root;
      } else if ($$3.startsWith("/")) {
         LinkFSPath $$4 = this.root;

         for (String $$5 : PATH_SPLITTER.split($$3.substring(1))) {
            if ($$5.isEmpty()) {
               throw new IllegalArgumentException("Empty paths not allowed");
            }

            $$4 = $$4.resolveName($$5);
         }

         return $$4;
      } else {
         LinkFSPath $$6 = null;

         for (String $$7 : PATH_SPLITTER.split($$3)) {
            if ($$7.isEmpty()) {
               throw new IllegalArgumentException("Empty paths not allowed");
            }

            $$6 = new LinkFSPath(this, $$7, $$6, PathContents.RELATIVE);
         }

         if ($$6 == null) {
            throw new IllegalArgumentException("Empty paths not allowed");
         } else {
            return $$6;
         }
      }
   }

   @Override
   public PathMatcher getPathMatcher(String pSyntaxAndPattern) {
      throw new UnsupportedOperationException();
   }

   @Override
   public UserPrincipalLookupService getUserPrincipalLookupService() {
      throw new UnsupportedOperationException();
   }

   @Override
   public WatchService newWatchService() {
      throw new UnsupportedOperationException();
   }

   public FileStore store() {
      return this.store;
   }

   public LinkFSPath rootPath() {
      return this.root;
   }

   public static LinkFileSystem.Builder builder() {
      return new LinkFileSystem.Builder();
   }

   public static class Builder {
      private final LinkFileSystem.DirectoryEntry root = new LinkFileSystem.DirectoryEntry();

      public LinkFileSystem.Builder put(List<String> pPathString, String pFileName, Path pFilePath) {
         LinkFileSystem.DirectoryEntry $$3 = this.root;

         for (String $$4 : pPathString) {
            $$3 = $$3.children.computeIfAbsent($$4, p_249671_ -> new LinkFileSystem.DirectoryEntry());
         }

         $$3.files.put(pFileName, pFilePath);
         return this;
      }

      public LinkFileSystem.Builder put(List<String> pPathString, Path pFilePath) {
         if (pPathString.isEmpty()) {
            throw new IllegalArgumentException("Path can't be empty");
         } else {
            int $$2 = pPathString.size() - 1;
            return this.put(pPathString.subList(0, $$2), pPathString.get($$2), pFilePath);
         }
      }

      public FileSystem build(String pName) {
         return new LinkFileSystem(pName, this.root);
      }
   }

   static record DirectoryEntry(Map<String, LinkFileSystem.DirectoryEntry> children, Map<String, Path> files) {
      public DirectoryEntry() {
         this(new HashMap<>(), new HashMap<>());
      }
   }
}
