import {onDocumentCreated} from "firebase-functions/v2/firestore";
import * as admin from "firebase-admin";
import * as logger from "firebase-functions/logger";

admin.initializeApp();

// Robot 1: Untuk Notifikasi Reminder
export const sendReminderNotification = onDocumentCreated(
  "notification_requests/{requestId}",
  async (event) => {
    const snap = event.data;
    if (!snap) {
      logger.log("No data associated with the event");
      return;
    }
    const requestData = snap.data();
    const toUserId = requestData.toUserId;
    const fromUserId = requestData.fromUserId;
    const message = requestData.message;

    const fromUserDoc = await admin.firestore()
      .collection("users").doc(fromUserId).get();
    const fromUserName = fromUserDoc.data()?.nama || "Seseorang";

    const toUserDoc = await admin.firestore()
      .collection("users").doc(toUserId).get();
    const fcmToken = toUserDoc.data()?.fcmToken;

    if (!fcmToken) {
      logger.log(`No FCM token for user ${toUserId}.`);
      await snap.ref.delete();
      return;
    }

    const payload = {
      notification: {
        title: `Reminder dari ${fromUserName} ❤️`,
        body: message,
      },
    };

    try {
      await admin.messaging().send({
        token: fcmToken,
        notification: payload.notification,
      });
      logger.log("Reminder notification sent successfully.");
      await snap.ref.delete();
    } catch (error) {
      logger.error("Error sending reminder notification:", error);
    }
  },
);

// Robot 2: Untuk Notifikasi Chat
export const sendChatMessageNotification = onDocumentCreated(
  "chats/{chatId}/messages/{messageId}",
  async (event) => {
    const snap = event.data;
    if (!snap) {
      logger.log("No data associated with the chat message event");
      return;
    }
    const messageData = snap.data();
    const senderId = messageData.senderId;
    const messageText = messageData.text;

    const chatId = event.params.chatId;
    const userIds = chatId.split("_");
    const recipientId = userIds.find((id) => id !== senderId);

    if (!recipientId) {
      logger.log("Could not find recipient ID.");
      return;
    }

    const senderDoc = await admin.firestore()
      .collection("users").doc(senderId).get();
    const senderName = senderDoc.data()?.nama || "Seseorang";

    const recipientDoc = await admin.firestore()
      .collection("users").doc(recipientId).get();
    const recipientToken = recipientDoc.data()?.fcmToken;

    if (!recipientToken) {
      logger.log(`No FCM token for recipient ${recipientId}.`);
      return;
    }

    const payload = {
      notification: {
        title: `Pesan baru dari ${senderName}`,
        body: messageText,
      },
    };

    try {
      await admin.messaging().send({
        token: recipientToken,
        notification: payload.notification,
      });
      logger.log("Chat notification sent successfully.");
    } catch (error) {
      logger.error("Error sending chat notification:", error);
    }
  },
);
