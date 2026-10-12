package net.minecraft.client.multiplayer.chat.report;

import com.google.common.collect.Lists;
import com.mojang.authlib.minecraft.report.AbuseReport;
import com.mojang.authlib.minecraft.report.AbuseReportLimits;
import com.mojang.authlib.minecraft.report.ReportChatMessage;
import com.mojang.authlib.minecraft.report.ReportEvidence;
import com.mojang.authlib.minecraft.report.ReportedEntity;
import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.Optionull;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.reporting.ChatReportScreen;
import net.minecraft.client.multiplayer.chat.LoggedChatMessage;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.SignedMessageBody;
import net.minecraft.network.chat.SignedMessageLink;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.StringUtils;

@OnlyIn(Dist.CLIENT)
public class ChatReport extends Report {
   final IntSet reportedMessages = new IntOpenHashSet();

   ChatReport(UUID pReportId, Instant pCreatedAt, UUID pReportedProfileId) {
      super(pReportId, pCreatedAt, pReportedProfileId);
   }

   public void toggleReported(int pId, AbuseReportLimits pLimits) {
      if (this.reportedMessages.contains(pId)) {
         this.reportedMessages.remove(pId);
      } else if (this.reportedMessages.size() < pLimits.maxReportedMessageCount()) {
         this.reportedMessages.add(pId);
      }
   }

   public ChatReport copy() {
      ChatReport $$0 = new ChatReport(this.reportId, this.createdAt, this.reportedProfileId);
      $$0.reportedMessages.addAll(this.reportedMessages);
      $$0.comments = this.comments;
      $$0.reason = this.reason;
      $$0.attested = this.attested;
      return $$0;
   }

   @Override
   public Screen createScreen(Screen pLastScreen, ReportingContext pReportingContext) {
      return new ChatReportScreen(pLastScreen, pReportingContext, this);
   }

   @OnlyIn(Dist.CLIENT)
   public static class Builder extends Report.Builder<ChatReport> {
      public Builder(ChatReport pReport, AbuseReportLimits pLimits) {
         super(pReport, pLimits);
      }

      public Builder(UUID pReportedProfileId, AbuseReportLimits pLimits) {
         super(new ChatReport(UUID.randomUUID(), Instant.now(), pReportedProfileId), pLimits);
      }

      public IntSet reportedMessages() {
         return this.report.reportedMessages;
      }

      public void toggleReported(int pId) {
         this.report.toggleReported(pId, this.limits);
      }

      public boolean isReported(int pId) {
         return this.report.reportedMessages.contains(pId);
      }

      @Override
      public boolean hasContent() {
         return StringUtils.isNotEmpty(this.comments()) || !this.reportedMessages().isEmpty() || this.reason() != null;
      }

      @Nullable
      @Override
      public Report.CannotBuildReason checkBuildable() {
         if (this.report.reportedMessages.isEmpty()) {
            return Report.CannotBuildReason.NO_REPORTED_MESSAGES;
         } else if (this.report.reportedMessages.size() > this.limits.maxReportedMessageCount()) {
            return Report.CannotBuildReason.TOO_MANY_MESSAGES;
         } else if (this.report.reason == null) {
            return Report.CannotBuildReason.NO_REASON;
         } else {
            return this.report.comments.length() > this.limits.maxOpinionCommentsLength() ? Report.CannotBuildReason.COMMENT_TOO_LONG : super.checkBuildable();
         }
      }

      @Override
      public Either<Report.Result, Report.CannotBuildReason> build(ReportingContext pReportingContext) {
         Report.CannotBuildReason $$1 = this.checkBuildable();
         if ($$1 != null) {
            return Either.right($$1);
         } else {
            String $$2 = Objects.requireNonNull(this.report.reason).backendName();
            ReportEvidence $$3 = this.buildEvidence(pReportingContext);
            ReportedEntity $$4 = new ReportedEntity(this.report.reportedProfileId);
            AbuseReport $$5 = AbuseReport.chat(this.report.comments, $$2, $$3, $$4, this.report.createdAt);
            return Either.left(new Report.Result(this.report.reportId, ReportType.CHAT, $$5));
         }
      }

      private ReportEvidence buildEvidence(ReportingContext pReportingContext) {
         List<ReportChatMessage> $$1 = new ArrayList<>();
         ChatReportContextBuilder $$2 = new ChatReportContextBuilder(this.limits.leadingContextMessageCount());
         $$2.collectAllContext(
            pReportingContext.chatLog(),
            this.report.reportedMessages,
            (p_299095_, p_300385_) -> $$1.add(this.buildReportedChatMessage(p_300385_, this.isReported(p_299095_)))
         );
         return new ReportEvidence(Lists.reverse($$1));
      }

      private ReportChatMessage buildReportedChatMessage(LoggedChatMessage.Player pChatMessage, boolean pMessageReported) {
         SignedMessageLink $$2 = pChatMessage.message().link();
         SignedMessageBody $$3 = pChatMessage.message().signedBody();
         List<ByteBuffer> $$4 = $$3.lastSeen().entries().stream().map(MessageSignature::asByteBuffer).toList();
         ByteBuffer $$5 = Optionull.map(pChatMessage.message().signature(), MessageSignature::asByteBuffer);
         return new ReportChatMessage($$2.index(), $$2.sender(), $$2.sessionId(), $$3.timeStamp(), $$3.salt(), $$4, $$3.content(), $$5, pMessageReported);
      }

      public ChatReport.Builder copy() {
         return new ChatReport.Builder(this.report.copy(), this.limits);
      }
   }
}
