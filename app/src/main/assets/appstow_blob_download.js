(() => {
  if (window.top !== window
      || window.__appStowBlobDownloadInstalled) {
    return;
  }

  const bridge = window.AppStowBlobDownload;

  if (!bridge
      || typeof bridge.postMessage !== "function") {
    return;
  }

  window.__appStowBlobDownloadInstalled = true;

  const originalClick =
    HTMLAnchorElement.prototype.click;

  const CHUNK_SIZE = 64 * 1024;
  const MAX_SIZE = 512 * 1024 * 1024;

  let nextId = 0;
  let waiting = null;
  let transferQueue = Promise.resolve();

  bridge.onmessage = event => {
    let message;

    try {
      message = JSON.parse(event.data);
    } catch {
      return;
    }

    if (!waiting || message.id !== waiting.id) {
      return;
    }

    if (message.kind === waiting.expected) {
      waiting.resolve();
    } else if (message.kind === "error") {
      waiting.reject(new Error("Native download failed"));
    }
  };

  function sendAndWait(payload, id, expected) {
    return new Promise((resolve, reject) => {
      const timeout = setTimeout(() => {
        waiting = null;
        reject(new Error("Download timeout"));
      }, 60000);

      waiting = {
        id,
        expected,

        resolve: () => {
          clearTimeout(timeout);
          waiting = null;
          resolve();
        },

        reject: error => {
          clearTimeout(timeout);
          waiting = null;
          reject(error);
        }
      };

      try {
        bridge.postMessage(payload);
      } catch (error) {
        waiting.reject(error);
      }
    });
  }

  async function transfer(captured, fileName, id) {
    try {
      const result = await captured;

      if (result.error) {
        throw result.error;
      }

      const blob = result.blob;

      if (blob.size > MAX_SIZE) {
        throw new Error("File exceeds download limit");
      }

      await sendAndWait(
        JSON.stringify({
          kind: "start",
          id,
          fileName,
          mimeType: blob.type || "application/octet-stream"
        }),
        id,
        "ready"
      );

      for (let offset = 0; offset < blob.size;
           offset += CHUNK_SIZE) {

        const chunk = await blob
          .slice(offset, offset + CHUNK_SIZE)
          .arrayBuffer();

        await sendAndWait(
          chunk,
          id,
          "next"
        );
      }

      await sendAndWait(
        JSON.stringify({ kind: "end", id }),
        id,
        "done"
      );

    } catch {
      bridge.postMessage(
        JSON.stringify({ kind: "abort", id })
      );
    }
  }

  HTMLAnchorElement.prototype.click = function() {
    const href = this.href;

    if (typeof href === "string"
        && href.startsWith("blob:")
        && this.hasAttribute("download")) {

      const fileName =
        this.getAttribute("download") || "download";

      const id = ++nextId;

      // Fetch immediately, before the page revokes the URL.
      const captured = fetch(href).then(
        response => {
          if (!response.ok) {
            return { error: new Error("Blob fetch failed") };
          }

          return response.blob().then(
            blob => ({ blob }),
            error => ({ error })
          );
        },
        error => ({ error })
      );

      transferQueue = transferQueue
        .catch(() => {})
        .then(() => transfer(captured, fileName, id));

      return;
    }

    return originalClick.apply(this, arguments);
  };
})();
