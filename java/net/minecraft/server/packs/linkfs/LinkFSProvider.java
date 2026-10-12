package net.minecraft.server.packs.linkfs;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessDeniedException;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;
import java.nio.file.ReadOnlyFileSystemException;
import java.nio.file.StandardOpenOption;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

class LinkFSProvider extends FileSystemProvider {
   public static final String SCHEME = "x-mc-link";

   @Override
   public String getScheme() {
      return "x-mc-link";
   }

   @Override
   public FileSystem newFileSystem(URI pUri, Map<String, ?> pEnviroment) {
      throw new UnsupportedOperationException();
   }

   @Override
   public FileSystem getFileSystem(URI pUri) {
      throw new UnsupportedOperationException();
   }

   @Override
   public Path getPath(URI pUri) {
      throw new UnsupportedOperationException();
   }

   @Override
   public SeekableByteChannel newByteChannel(Path pPath, Set<? extends OpenOption> pOptions, FileAttribute<?>... pAttributes) throws IOException {
      if (!pOptions.contains(StandardOpenOption.CREATE_NEW)
         && !pOptions.contains(StandardOpenOption.CREATE)
         && !pOptions.contains(StandardOpenOption.APPEND)
         && !pOptions.contains(StandardOpenOption.WRITE)) {
         Path $$3 = toLinkPath(pPath).toAbsolutePath().getTargetPath();
         if ($$3 == null) {
            throw new NoSuchFileException(pPath.toString());
         } else {
            return Files.newByteChannel($$3, pOptions, pAttributes);
         }
      } else {
         throw new UnsupportedOperationException();
      }
   }

   @Override
   public DirectoryStream<Path> newDirectoryStream(Path pDirectory, final Filter<? super Path> pFilter) throws IOException {
      final PathContents.DirectoryContents $$2 = toLinkPath(pDirectory).toAbsolutePath().getDirectoryContents();
      if ($$2 == null) {
         throw new NotDirectoryException(pDirectory.toString());
      } else {
         return new DirectoryStream<Path>() {
            @Override
            public Iterator<Path> iterator() {
               return $$2.children().values().stream().filter(p_250987_ -> {
                  try {
                     return pFilter.accept(p_250987_);
                  } catch (IOException var3) {
                     throw new DirectoryIteratorException(var3);
                  }
               }).map(p_249891_ -> (Path)p_249891_).iterator();
            }

            @Override
            public void close() {
            }
         };
      }
   }

   @Override
   public void createDirectory(Path pPath, FileAttribute<?>... pAttributes) {
      throw new ReadOnlyFileSystemException();
   }

   @Override
   public void delete(Path pPath) {
      throw new ReadOnlyFileSystemException();
   }

   @Override
   public void copy(Path pSource, Path pTarget, CopyOption... pOptions) {
      throw new ReadOnlyFileSystemException();
   }

   @Override
   public void move(Path pSource, Path pTarget, CopyOption... pOptions) {
      throw new ReadOnlyFileSystemException();
   }

   @Override
   public boolean isSameFile(Path pPath, Path pPath2) {
      return pPath instanceof LinkFSPath && pPath2 instanceof LinkFSPath && pPath.equals(pPath2);
   }

   @Override
   public boolean isHidden(Path pPath) {
      return false;
   }

   @Override
   public FileStore getFileStore(Path pPath) {
      return toLinkPath(pPath).getFileSystem().store();
   }

   @Override
   public void checkAccess(Path pPath, AccessMode... pModes) throws IOException {
      if (pModes.length == 0 && !toLinkPath(pPath).exists()) {
         throw new NoSuchFileException(pPath.toString());
      } else {
         AccessMode[] var3 = pModes;
         int var4 = pModes.length;
         int var5 = 0;

         while (var5 < var4) {
            AccessMode $$2 = var3[var5];
            switch ($$2) {
               case READ:
                  if (!toLinkPath(pPath).exists()) {
                     throw new NoSuchFileException(pPath.toString());
                  }
               default:
                  var5++;
                  break;
               case EXECUTE:
               case WRITE:
                  throw new AccessDeniedException($$2.toString());
            }
         }
      }
   }

   @Nullable
   @Override
   public <V extends FileAttributeView> V getFileAttributeView(Path pPath, Class<V> pType, LinkOption... pOptions) {
      LinkFSPath $$3 = toLinkPath(pPath);
      return (V)(pType == BasicFileAttributeView.class ? $$3.getBasicAttributeView() : null);
   }

   @Override
   public <A extends BasicFileAttributes> A readAttributes(Path pPath, Class<A> pType, LinkOption... pOptions) throws IOException {
      LinkFSPath $$3 = toLinkPath(pPath).toAbsolutePath();
      if (pType == BasicFileAttributes.class) {
         return (A)$$3.getBasicAttributes();
      } else {
         throw new UnsupportedOperationException("Attributes of type " + pType.getName() + " not supported");
      }
   }

   @Override
   public Map<String, Object> readAttributes(Path pPath, String pAttributes, LinkOption... pOptions) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void setAttribute(Path pPath, String pAttribute, Object pValue, LinkOption... pOptions) {
      throw new ReadOnlyFileSystemException();
   }

   private static LinkFSPath toLinkPath(@Nullable Path pPath) {
      if (pPath == null) {
         throw new NullPointerException();
      } else if (pPath instanceof LinkFSPath) {
         return (LinkFSPath)pPath;
      } else {
         throw new ProviderMismatchException();
      }
   }
}
