/* =========================================================================
   Glassmorphic OS Chat UI Logic — AI Music & Movies Assistant
   ========================================================================= */

document.addEventListener("DOMContentLoaded", () => {
    const chatInput = document.getElementById("chat-input");
    const sendButton = document.getElementById("send-button");
    const messagesContainer = document.getElementById("messages");
    const loadingIndicator = document.getElementById("loading-indicator");

    // Endpoint for Rasa's REST API Channel 
    const RASA_API_URL = "http://localhost:5005/webhooks/rest/webhook";

    // Submits on "Enter"
    chatInput.addEventListener("keypress", (e) => {
        if (e.key === "Enter" && !e.shiftKey) {
            e.preventDefault();
            handleSend();
        }
    });

    sendButton.addEventListener("click", () => {
        handleSend();
    });

    async function handleSend() {
        const text = chatInput.value.trim();
        if (!text) return;

        // 1. Immediately render User message
        renderMessage(text, "user");
        chatInput.value = "";
        
        // 2. Show loading dots indicator
        toggleLoading(true);

        try {
            // 3. Fire payload asynchronously mapping towards Rasa pipeline
            const response = await fetch(RASA_API_URL, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    sender: "user_bhargav",
                    message: text
                })
            });

            if (!response.ok) throw new Error("Network response was not OK");
            
            // 4. Extract serialized responses
            const rasaResponses = await response.json();
            
            toggleLoading(false);

            // 5. Render Bot outputs handling edge case for silent arrays
            if (rasaResponses && rasaResponses.length > 0) {
                for (const msg of rasaResponses) {
                    if (msg.text) {
                        renderMessage(msg.text, "bot");
                    }
                    if (msg.image) {
                        // Additional handling if Rasa returns raw image links
                        renderMessage(`<img src="${msg.image}" alt="Bot Output Image" style="max-width: 100%; border-radius: 12px; margin-top: 8px;">`, "bot", true);
                    }
                }
            } else {
                renderMessage("*(No response returned from the server...)*", "bot");
            }
        } catch (error) {
            toggleLoading(false);
            console.error("Rasa API Error:", error);
            renderMessage("⚠️ Connection Error. Ensure `rasa run --enable-api --cors '*'` is running!", "bot");
        }
    }

    /**
     * DOM node factory generating styled Chat elements natively injected into `.messages`
     */
    function renderMessage(content, senderType, isHTML = false) {
        // Build encasement framework preserving hierarchy structural logic
        const messageDiv = document.createElement("div");
        messageDiv.className = `message ${senderType}-message fade-in-up`;

        const bubbleDiv = document.createElement("div");
        bubbleDiv.className = "bubble";

        if (isHTML) {
            bubbleDiv.innerHTML = content;
        } else {
            // Use textContent directly avoiding XSS injections natively
            bubbleDiv.textContent = content;
        }

        messageDiv.appendChild(bubbleDiv);

        // Inject sequentially BEFORE the loading indicator to preserve UI stacking
        messagesContainer.insertBefore(messageDiv, loadingIndicator);
        
        // Ensure browser scrolls down to the newly appended message cleanly
        messagesContainer.scrollTop = messagesContainer.scrollHeight;
    }

    /**
     * Toggles bot simulation dot indicators dynamically mapping display states.
     */
    function toggleLoading(show) {
        if (show) {
            loadingIndicator.classList.remove("hidden");
            // Automatically push loading dots into bottom view
            messagesContainer.scrollTop = messagesContainer.scrollHeight;
        } else {
            loadingIndicator.classList.add("hidden");
        }
    }
});
