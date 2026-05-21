import { onDocumentCreated } from "firebase-functions/v2/firestore";
import { initializeApp } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";
import { getMessaging } from "firebase-admin/messaging";

initializeApp();

const db = getFirestore();

/**
 * Cloud Function unificada bajo nomenclatura del dominio (inglés).
 */
export const enviarNotificacionPush = onDocumentCreated(
  "notification_requests/{docId}",
  async (event) => {

    const solicitud = event.data?.data();
    const docId = event.params.docId;

    if (!solicitud) {
      console.error("Solicitud vacía.");
      return null;
    }

    if (solicitud.procesado) {
      console.log(`Solicitud ${docId} ya procesada.`);
      return null;
    }

    // ✅ CORREGIDO: Mismos nombres que tiene el documento en Firestore (según NotificationSender.kt)
    const {
      userId,
      type,
      title,
      body,
      relatedEventId = "",
      extraData = {}
    } = solicitud;

    // Control preventivo
    if (!userId || userId.trim() === "") {
      console.error(`Solicitud ${docId} no contiene un userId válido.`);
      await marcarProcesado(docId, false, "El campo userId está vacío o ausente.");
      return null;
    }

    // ─────────────────────────────────────────────
    // PASO 1 — Guardar SIEMPRE en historial
    // ─────────────────────────────────────────────
    try {
      await db.collection("notifications").add({
        userId: userId,
        title: title || "Sin título",
        body: body || "",
        timestamp: Date.now(),
        isRead: false,
        type: type || "GENERAL",
        relatedEventId: relatedEventId
      });

      console.log(`Notificación guardada en historial para usuario ${userId}`);

    } catch (error: any) {
      console.error(`Error guardando historial: ${error.message}`);
    }

    // ─────────────────────────────────────────────
    // PASO 2 — Buscar token FCM
    // ─────────────────────────────────────────────
    try {
      const usuarioDoc = await db
        .collection("users")
        .doc(userId)
        .get();

      if (!usuarioDoc.exists) {
        console.error(`Usuario ${userId} no encontrado.`);
        await marcarProcesado(docId, false, "Usuario no encontrado");
        return null;
      }

      const fcmToken = usuarioDoc.data()?.fcmToken;

      if (!fcmToken || fcmToken.trim() === "") {
        console.warn(`Usuario ${userId} sin token FCM.`);
        await marcarProcesado(docId, false, "Token no disponible");
        return null;
      }

      // ─────────────────────────────────────────
      // PASO 3 — Enviar push
      // ─────────────────────────────────────────
      const payloadData: Record<string, string> = {
        type: type || "GENERAL",
        userId: userId,
        relatedEventId: relatedEventId,
        timestamp: Date.now().toString(),
      };

      // Limpieza de extraData para FCM (Solo Strings)
      if (extraData && typeof extraData === "object" && !Array.isArray(extraData)) {
        for (const [key, value] of Object.entries(extraData)) {
          if (value !== undefined && value !== null) {
            payloadData[key] = String(value);
          }
        }
      }

      const mensaje = {
        token: fcmToken,
        notification: {
          title: title, // ✅ Ahora sí tiene el string real
          body: body    // ✅ Ahora sí tiene el string real
        },
        data: payloadData,
        android: {
          priority: "high" as const,
          notification: {
            channelId: "comuniapp_channel"
          }
        }
      };

      const response = await getMessaging().send(mensaje);
      console.log(`Push enviado con éxito: ${response}`);

      await marcarProcesado(docId, true, null);
      return null;

    } catch (error: any) {
      console.error(`Error enviando push: ${error.message}`);
      await marcarProcesado(docId, false, error.message);
      return null;
    }
  }
);

/**
 * Marca la solicitud como procesada.
 */
async function marcarProcesado(
  docId: string,
  exito: boolean,
  error: string | null
) {
  await db
    .collection("notification_requests")
    .doc(docId)
    .update({
      procesado: true,
      procesadoExito: exito,
      procesadoError: error,
      procesadoTimestamp: Date.now()
    });
}