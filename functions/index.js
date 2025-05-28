const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const { defineString } = require("firebase-functions/params");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");

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