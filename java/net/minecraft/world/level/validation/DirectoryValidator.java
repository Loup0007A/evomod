package net.minecraft.world.level.validation;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class DirectoryValidator {
   private final PathMatcher symlinkTargetAllowList;

   public DirectoryValidator(PathMatcher pSymlinkTargetAllowList) {
      this.symlinkTargetAllowList = pSymlinkTargetAllowList;
   }

   public void validateSymlink(Path pDirectory, List<ForbiddenSymlinkInfo> pEntries) throws IOException {
      Path $$2 = Files.readSymbolicLink(pDirectory);
      if (!this.symlinkTargetAllowList.matches($$2)) {
         pEntries.add(new ForbiddenSymlinkInfo(pDirectory, $$2));
      }
   }

   public List<ForbiddenSymlinkInfo> validateSymlink(Path pDirectory) throws IOException {
      List<ForbiddenSymlinkInfo> $$1 = new ArrayList<>();
      this.validateSymlink(pDirectory, $$1);
      return $$1;
   }

   public List<ForbiddenSymlinkInfo> validateDirectory(Path pDirectory, boolean pValidateSymlinks) throws IOException {
      List<ForbiddenSymlinkInfo> $$2 = new ArrayList<>();

      BasicFileAttributes $$3;
      try {
         $$3 = Files.readAttributes(pDirectory, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
      } catch (NoSuchFileException var6) {
         return $$2;
      }

      if ($$3.isRegularFile()) {
         throw new IOException("Path " + pDirectory + " is not a directory");
      } else {
         if ($$3.isSymbolicLink()) {
            if (!pValidateSymlinks) {
               this.validateSymlink(pDirectory, $$2);
               return $$2;
            }

            pDirectory = Files.readSymbolicLink(pDirectory);
         }

         this.validateKnownDirectory(pDirectory, $$2);
         return $$2;
      }
   }

   public void validateKnownDirectory(Path pDirectory, final List<ForbiddenSymlinkInfo> pForbiddenSymlinkInfos) throws IOException {
      Files.walkFileTree(pDirectory, new SimpleFileVisitor<Path>() {
         private void validateSymlink(Path p_289935_, BasicFileAttributes p_289941_) throws IOException {
            if (p_289941_.isSymbolicLink()) {
               DirectoryValidator.this.validateSymlink(p_289935_, pForbiddenSymlinkInfos);
            }
         }

         public FileVisitResult preVisitDirectory(Path p_289946_, BasicFileAttributes p_289950_) throws IOException {
            this.validateSymlink(p_289946_, p_289950_);
            return super.preVisitDirectory(p_289946_, p_289950_);
         }

         public FileVisitResult visitFile(Path p_289986_, BasicFileAttributes p_289991_) throws IOException {
            this.validateSymlink(p_289986_, p_289991_);
            return super.visitFile(p_289986_, p_289991_);
         }
      });
   }
}
