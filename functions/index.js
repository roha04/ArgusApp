// Import Firebase Admin
const admin = require('firebase-admin');
const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");

// Import Firebase Functions v2
const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const { onCall } = require("firebase-functions/v2/https");
const { defineString } = require("firebase-functions/params");

// Initialize Firebase
initializeApp();
const db = getFirestore();

// Run security analysis on new activity logs
exports.analyzeActivityLog = onDocumentCreated("activity_logs/{logId}", async (event) => {
  const log = event.data.data();
  const logId = event.params.logId;

  // Skip analysis for certain system activities
  if (log.action === "System Maintenance" || log.action === "Automated Backup") {
    return null;
  }

  // Get user's activity history
  const userLogsSnapshot = await db.collection("activity_logs")
    .where("userId", "==", log.userId)
    .orderBy("timestamp", "desc")
    .limit(100)
    .get();

  const userLogs = userLogsSnapshot.docs.map((doc) => {
    const data = doc.data();
    data.id = doc.id;
    return data;
  });

  // Initialize alerts array
  const alerts = [];

  // 1. Check for login anomalies
  if (log.action === "Вхід в систему") {
    // Check for unusual login time
    const hour = new Date(log.timestamp.toDate()).getHours();
    const isBusinessHours = hour >= 8 && hour <= 19;

    if (!isBusinessHours) {
      alerts.push({
        alertType: "unusual_time",
        severity: 3,
        description: "Вхід в нетиповий час",
        details: `Користувач увійшов в систему о ${hour}:00. Типовий час входу: 8:00-19:00.`,
        relatedLogIds: [logId],
      });
    }

    // Check for geolocation anomalies (simplified)
    if (log.location !== "Unknown" && userLogs.length > 1) {
      const previousLocations = userLogs
        .filter((l) => l.location !== "Unknown" && l.id !== logId)
        .map((l) => l.location);

      if (previousLocations.length > 0 && !previousLocations.includes(log.location)) {
        alerts.push({
          alertType: "geolocation_anomaly",
          severity: 4,
          description: "Вхід з нової локації",
          details: `Користувач увійшов з локації "${log.location}", яка відрізняється від попередніх локацій.`,
          relatedLogIds: [logId],
        });
      }
    }
  }

  // 2. Check for excessive activity
  const recentLogs = userLogs.filter((l) =>
    l.timestamp.toDate() > new Date(Date.now() - 30 * 60000) // Last 30 minutes
  );

  if (recentLogs.length > 50) {
    alerts.push({
      alertType: "excessive_activity",
      severity: 3,
      description: "Надмірна активність",
      details: `Користувач виконав ${recentLogs.length} дій за останні 30 хвилин.`,
      relatedLogIds: recentLogs.map((l) => l.id || ""),
    });
  }

  // 3. Check for multiple failures
  const recentFailures = userLogs.filter((l) =>
    !l.isSuccessful &&
    l.timestamp.toDate() > new Date(Date.now() - 15 * 60000) // Last 15 minutes
  );

  if (recentFailures.length >= 5) {
    alerts.push({
      alertType: "multiple_failures",
      severity: 4,
      description: "Багаторазові невдачі",
      details: `Користувач мав ${recentFailures.length} невдалих спроб за останні 15 хвилин.`,
      relatedLogIds: recentFailures.map((l) => l.id || ""),
    });
  }

  // Create security alerts in Firestore
  const promises = alerts.map((alert) => {
    const alertData = {
      userId: log.userId,
      userEmail: log.userEmail,
      userType: log.userType,
      alertType: alert.alertType,
      severity: alert.severity,
      description: alert.description,
      details: alert.details,
      timestamp: new Date(),
      relatedLogIds: alert.relatedLogIds.filter((id) => id),
      status: "new",
    };

    return db.collection("security_alerts").add(alertData);
  });

  return Promise.all(promises);
});

// Daily analysis function (runs once per day)
exports.dailySecurityAnalysis = onSchedule({
  schedule: "0 1 * * *", // Run at 1 AM every day
  timeZone: "Europe/Kiev",
}, async (event) => {
  // Get all users
  const usersSnapshot = await db.collection("users").get();
  const users = usersSnapshot.docs.map((doc) => {
    const data = doc.data();
    return {id: doc.id, ...data};
  });

  const yesterday = new Date();
  yesterday.setDate(yesterday.getDate() - 1);

  const alerts = [];

  // Analyze each user's activity
  for (const user of users) {
    // Get yesterday's logs for this user
    const logsSnapshot = await db.collection("activity_logs")
      .where("userId", "==", user.id)
      .where("timestamp", ">", yesterday)
      .get();

    const logs = logsSnapshot.docs.map((doc) => doc.data());

    // Skip users with no activity
    if (logs.length === 0) continue;

    // Get user's average activity (from the last 30 days)
    const thirtyDaysAgo = new Date();
    thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30);

    const historicalLogsSnapshot = await db.collection("activity_logs")
      .where("userId", "==", user.id)
      .where("timestamp", ">", thirtyDaysAgo)
      .where("timestamp", "<", yesterday)
      .get();

    const historicalLogs = historicalLogsSnapshot.docs.map((doc) => doc.data());

    // Calculate average daily actions
    const days = Math.max(1, Math.ceil((Date.now() - thirtyDaysAgo.getTime()) /
      (24 * 60 * 60 * 1000)));
    const avgDailyActions = historicalLogs.length / days;

    // Alert if yesterday's activity was 3x the average
    if (logs.length > avgDailyActions * 3 && logs.length > 20) {
      alerts.push({
        userId: user.id,
        userEmail: user.email,
        userType: user.role === "admin" ? "Адміністратор" :
                  user.role === "police" ? "Поліцейський" : "Громадянин",
        alertType: "excessive_activity",
        severity: 3,
        description: "Аномальна денна активність",
        details: `Користувач виконав ${logs.length} дій вчора, що в ${(logs.length / avgDailyActions).toFixed(1)} разів більше за середню денну активність (${avgDailyActions.toFixed(1)}).`,
        timestamp: new Date(),
        relatedLogIds: logsSnapshot.docs.map((doc) => doc.id),
        status: "new",
      });
    }
  }

  // Create security alerts in Firestore
  const promises = alerts.map((alert) => {
    return db.collection("security_alerts").add(alert);
  });

  return Promise.all(promises);
});

// Mass messaging function - SIMPLIFIED VERSION
exports.sendMassMessage = onCall({
  enforceAppCheck: false
}, async (request) => {
  try {
    console.log("sendMassMessage function called");

    // Check authentication
    if (!request.auth) {
      console.log("Error: User not authenticated");
      throw new Error('User must be authenticated to send mass messages');
    }

    // Get the user's role from Firestore
    const userDoc = await db.collection('users').doc(request.auth.uid).get();
    if (!userDoc.exists || userDoc.data().role !== 'admin') {
      console.log("Permission error: User is not an admin");
      throw new Error('Only administrators can send mass messages');
    }

    // Get message data
    const { title, body, topic = "all", userType = "all" } = request.data;

    if (!title || !body) {
      console.log("Error: Missing title or body");
      throw new Error('Title and body are required');
    }

    console.log("Message data:", { title, body, topic, userType });

    // Prepare the message
    const message = {
      notification: {
        title: title,
        body: body,
      },
      data: {
        // We don't have a messageId since we're skipping Firestore
        clickAction: 'OPEN_NOTIFICATION_ACTIVITY',
        type: 'mass_message'
      },
      android: {
        priority: 'high',
        notification: {
          sound: 'default',
          priority: 'high',
          channelId: 'general_notifications'
        }
      },
    };

    // Determine target based on userType
    let targetTopic;
    if (userType && userType !== 'all') {
      // Send to specific user type (admin, police, citizen)
      targetTopic = `user_type_${userType}`;
    } else if (topic && topic !== 'all') {
      // Send to specific topic
      targetTopic = topic;
    } else {
      // Send to all users
      targetTopic = 'all_users';
    }

    message.topic = targetTopic;
    console.log(`Sending message to topic: ${targetTopic}`);

    // Send the message
    console.log("Sending FCM message");
    const response = await admin.messaging().send(message);
    console.log(`FCM message sent successfully: ${response}`);

    return {
      success: true,
      fcmResponse: response
    };
  } catch (error) {
    console.error('Error sending mass message:', error);
    throw new Error(`Failed to send message: ${error.message}`);
  }
});
exports.logMessageHistory = onCall({
  enforceAppCheck: false
}, async (request) => {
  try {
    // Authentication check
    if (!request.auth) {
      throw new Error('User must be authenticated');
    }

    const { title, body, topic, userType, fcmResponse } = request.data;

    // Log the message
    const messageDoc = await db.collection('mass_messages').add({
      title,
      body,
      sentBy: request.auth.uid,
      senderEmail: request.auth.token.email || 'Unknown',
      sentAt: admin.firestore.FieldValue.serverTimestamp(),
      targetTopic: topic || 'all',
      targetUserType: userType || 'all',
      deliveryStatus: 'sent',
      fcmResponse
    });

    return {
      success: true,
      messageId: messageDoc.id
    };
  } catch (error) {
    console.error('Error logging message history:', error);
    throw new Error(`Failed to log message history: ${error.message}`);
  }
});