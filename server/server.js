
const express = require("express");
const {
  initializeApp,
  applicationDefault,
} = require("firebase-admin/app");
const { getMessaging } = require("firebase-admin/messaging");

const app = express();
const PORT = process.env.PORT || 3000;

// Mã gia đình mặc định dùng cho bước thử nghiệm.
// Đổi giá trị này thành mã riêng của gia đình bạn.
const DEFAULT_FAMILY_CODE = process.env.DEFAULT_FAMILY_CODE || "FC-DEMO-2026";

// familyCode -> Map(token -> thông tin thiết bị)
const familyDevices = new Map();

app.use(express.json({ limit: "32kb" }));

initializeApp({
  credential: applicationDefault(),
});

function normalizeFamilyCode(value) {
  if (typeof value !== "string") return "";
  return value.trim().toUpperCase();
}

function isValidText(value) {
  return (
    typeof value === "string" &&
    value.trim().length > 0
  );
}

// Kiểm tra server
app.get("/", (req, res) => {
  res.json({
    success: true,
    message: "FamilyCare notification server is running",
  });
});

// Gửi thông báo đến một thiết bị cụ thể (giữ endpoint cũ)
app.post("/send-notification", async (req, res) => {
  try {
    const { token, title, body } = req.body;

    if (
      !isValidText(token) ||
      !isValidText(title) ||
      !isValidText(body)
    ) {
      return res.status(400).json({
        success: false,
        message: "Vui lòng cung cấp token, title và body hợp lệ.",
      });
    }

    const messageId = await getMessaging().send({
      token: token.trim(),
      notification: {
        title: title.trim(),
        body: body.trim(),
      },
      data: {
        title: title.trim(),
        body: body.trim(),
      },
      android: {
        priority: "high",
      },
    });

    return res.json({
      success: true,
      message: "Gửi thông báo thành công.",
      messageId,
    });
  } catch (error) {
    console.error("FCM error:", error.message);

    return res.status(500).json({
      success: false,
      message: "Không gửi được thông báo.",
      error: error.message,
    });
  }
});

// Đăng ký thiết bị người thân vào một gia đình
// Body: { "familyCode": "FC-DEMO-2026", "token": "FCM_TOKEN" }
app.post("/register-device", (req, res) => {
  const familyCode = normalizeFamilyCode(req.body.familyCode);
  const { token } = req.body;

  if (!familyCode || !isValidText(token)) {
    return res.status(400).json({
      success: false,
      message: "Cần cung cấp familyCode và token hợp lệ.",
    });
  }

  if (familyCode !== normalizeFamilyCode(DEFAULT_FAMILY_CODE)) {
    return res.status(403).json({
      success: false,
      message: "Mã liên kết gia đình không hợp lệ.",
    });
  }

  let devices = familyDevices.get(familyCode);

  if (!devices) {
    devices = new Map();
    familyDevices.set(familyCode, devices);
  }

  devices.set(token.trim(), {
    registeredAt: new Date().toISOString(),
  });

  console.log(
    `Đã đăng ký thiết bị cho gia đình ${familyCode}. Tổng thiết bị: ${devices.size}`
  );

  return res.json({
    success: true,
    message: "Đăng ký thiết bị thành công.",
  });
});

// Gửi cảnh báo đến tất cả thiết bị đã đăng ký trong gia đình
// Body: { "familyCode": "FC-DEMO-2026", "title": "...", "body": "..." }
app.post("/send-family-alert", async (req, res) => {
  const familyCode = normalizeFamilyCode(
    req.body.familyCode || DEFAULT_FAMILY_CODE
  );
  const { title, body } = req.body;

  if (!isValidText(title) || !isValidText(body)) {
    return res.status(400).json({
      success: false,
      message: "Tiêu đề hoặc nội dung cảnh báo không hợp lệ.",
    });
  }

  const devices = familyDevices.get(familyCode);

  if (!devices || devices.size === 0) {
    return res.status(404).json({
      success: false,
      message: "Chưa có thiết bị nào liên kết với gia đình này.",
    });
  }

  const tokens = [...devices.keys()];
  const invalidTokens = [];
  let successCount = 0;
  let failureCount = 0;

  // Gửi lần lượt để biết chính xác token nào hết hiệu lực.
  for (const token of tokens) {
    try {
      await getMessaging().send({
        token,
        notification: {
          title: title.trim(),
          body: body.trim(),
        },
        data: {
          type: "MEDICATION_OVERDUE",
          title: title.trim(),
          body: body.trim(),
        },
        android: {
          priority: "high",
        },
      });

      successCount++;
    } catch (error) {
      failureCount++;

      console.error("Family alert FCM error:", error.message);

      if (
        error.code === "messaging/registration-token-not-registered" ||
        error.code === "messaging/invalid-registration-token"
      ) {
        invalidTokens.push(token);
      }
    }
  }

  // Loại token đã hết hiệu lực
  for (const token of invalidTokens) {
    devices.delete(token);
  }

  return res.status(successCount > 0 ? 200 : 502).json({
    success: successCount > 0,
    message:
      successCount > 0
        ? "Đã gửi cảnh báo đến các thiết bị liên kết."
        : "Không gửi được cảnh báo đến thiết bị nào.",
    successCount,
    failureCount,
    removedInvalidTokens: invalidTokens.length,
  });
});

app.listen(PORT, "0.0.0.0", () => {
  console.log(`FamilyCare server running on port ${PORT}`);
  console.log(`Mã gia đình thử nghiệm: ${DEFAULT_FAMILY_CODE}`);
});
