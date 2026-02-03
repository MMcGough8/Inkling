/**
 * Inkling - Document Assistant
 * Main JavaScript Application
 */

// State
let sessionId = null;
let isLoading = false;

// DOM Elements
const uploadArea = document.getElementById('upload-area');
const fileInput = document.getElementById('file-input');
const uploadProgress = document.getElementById('upload-progress');
const documentList = document.getElementById('document-list');
const docCount = document.getElementById('doc-count');
const chatMessages = document.getElementById('chat-messages');
const chatForm = document.getElementById('chat-form');
const chatInput = document.getElementById('chat-input');
const sendButton = document.getElementById('send-button');
const toastContainer = document.getElementById('toast-container');

// ============================================
// INITIALIZATION
// ============================================

document.addEventListener('DOMContentLoaded', () => {
    loadDocuments();
    setupUpload();
    setupChat();
});

// ============================================
// DOCUMENT UPLOAD
// ============================================

function setupUpload() {
    // Click to upload
    uploadArea.addEventListener('click', () => fileInput.click());

    // File selected
    fileInput.addEventListener('change', (e) => {
        if (e.target.files.length > 0) {
            uploadFile(e.target.files[0]);
        }
    });

    // Drag and drop
    uploadArea.addEventListener('dragover', (e) => {
        e.preventDefault();
        uploadArea.classList.add('dragover');
    });

    uploadArea.addEventListener('dragleave', () => {
        uploadArea.classList.remove('dragover');
    });

    uploadArea.addEventListener('drop', (e) => {
        e.preventDefault();
        uploadArea.classList.remove('dragover');

        const files = e.dataTransfer.files;
        if (files.length > 0) {
            uploadFile(files[0]);
        }
    });
}

async function uploadFile(file) {
    // Validate file type
    const validTypes = ['application/pdf', 'text/plain', 'text/markdown'];
    const validExtensions = ['.pdf', '.txt', '.md'];
    const hasValidExtension = validExtensions.some(ext => file.name.toLowerCase().endsWith(ext));

    if (!validTypes.includes(file.type) && !hasValidExtension) {
        showToast('Please upload a PDF, TXT, or MD file', 'error');
        return;
    }

    // Show progress
    uploadProgress.classList.add('active');

    const formData = new FormData();
    formData.append('file', file);

    try {
        const response = await fetch('/api/documents', {
            method: 'POST',
            body: formData
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Upload failed');
        }

        const doc = await response.json();
        showToast(`"${doc.name}" uploaded successfully`, 'success');
        loadDocuments();

        // Clear welcome message on first upload
        const welcomeMsg = chatMessages.querySelector('.welcome-message');
        if (welcomeMsg && document.querySelectorAll('.document-item').length === 0) {
            // Keep welcome for now, it will be replaced when user chats
        }

    } catch (error) {
        showToast(error.message, 'error');
    } finally {
        uploadProgress.classList.remove('active');
        fileInput.value = '';
    }
}

// ============================================
// DOCUMENT LIST
// ============================================

async function loadDocuments() {
    try {
        const response = await fetch('/api/documents');
        const documents = await response.json();

        docCount.textContent = documents.length;

        if (documents.length === 0) {
            documentList.innerHTML = `
                <div class="empty-state">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                        <path d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path>
                    </svg>
                    <p>No documents yet.<br>Upload your first document to get started.</p>
                </div>
            `;
            return;
        }

        documentList.innerHTML = documents.map(doc => `
            <div class="document-item" data-id="${doc.id}">
                <div class="doc-icon">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"></path>
                        <polyline points="14 2 14 8 20 8"></polyline>
                        <line x1="16" y1="13" x2="8" y2="13"></line>
                        <line x1="16" y1="17" x2="8" y2="17"></line>
                        <polyline points="10 9 9 9 8 9"></polyline>
                    </svg>
                </div>
                <div class="doc-info">
                    <div class="doc-name" title="${doc.name}">${doc.name}</div>
                    <div class="doc-meta">
                        <span class="doc-status ${doc.status.toLowerCase()}">
                            <span class="status-dot"></span>
                            ${doc.status}
                        </span>
                        <span>${doc.chunkCount} chunks</span>
                    </div>
                </div>
                <button class="doc-delete" onclick="deleteDocument(${doc.id}, '${doc.name}')" title="Delete document">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <polyline points="3 6 5 6 21 6"></polyline>
                        <path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"></path>
                    </svg>
                </button>
            </div>
        `).join('');

    } catch (error) {
        showToast('Failed to load documents', 'error');
    }
}

async function deleteDocument(id, name) {
    if (!confirm(`Delete "${name}"? This cannot be undone.`)) {
        return;
    }

    try {
        const response = await fetch(`/api/documents/${id}`, {
            method: 'DELETE'
        });

        if (!response.ok) {
            throw new Error('Delete failed');
        }

        showToast(`"${name}" deleted`, 'success');
        loadDocuments();

    } catch (error) {
        showToast(error.message, 'error');
    }
}

// ============================================
// CHAT
// ============================================

function setupChat() {
    chatForm.addEventListener('submit', async (e) => {
        e.preventDefault();

        const question = chatInput.value.trim();
        if (!question || isLoading) return;

        // Clear welcome message
        const welcomeMsg = chatMessages.querySelector('.welcome-message');
        if (welcomeMsg) {
            welcomeMsg.remove();
        }

        // Add user message
        addMessage(question, 'user');

        // Clear input
        chatInput.value = '';

        // Show typing indicator
        showTypingIndicator();

        // Disable input
        setLoading(true);

        try {
            const response = await fetch('/api/chat', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    question: question,
                    sessionId: sessionId
                })
            });

            // Remove typing indicator
            removeTypingIndicator();

            if (!response.ok) {
                const error = await response.json();
                throw new Error(error.message || 'Failed to get response');
            }

            const data = await response.json();

            // Save session ID
            sessionId = data.sessionId;

            // Add assistant message
            addMessage(data.answer, 'assistant', data.sources);

        } catch (error) {
            removeTypingIndicator();
            showToast(error.message, 'error');
            addMessage('Sorry, I encountered an error. Please try again.', 'assistant');
        } finally {
            setLoading(false);
        }
    });

    // Handle Enter key
    chatInput.addEventListener('keydown', (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            chatForm.dispatchEvent(new Event('submit'));
        }
    });
}

function addMessage(content, role, sources = []) {
    const messageDiv = document.createElement('div');
    messageDiv.className = `message ${role}`;

    const avatarIcon = role === 'user'
        ? '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"></path><circle cx="12" cy="7" r="4"></circle></svg>'
        : '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2a2 2 0 012 2c0 .74-.4 1.39-1 1.73V7h1a7 7 0 017 7h1a1 1 0 011 1v3a1 1 0 01-1 1h-1v1a2 2 0 01-2 2H5a2 2 0 01-2-2v-1H2a1 1 0 01-1-1v-3a1 1 0 011-1h1a7 7 0 017-7h1V5.73c-.6-.34-1-.99-1-1.73a2 2 0 012-2z"></path><circle cx="9" cy="13" r="1"></circle><circle cx="15" cy="13" r="1"></circle><path d="M9 17h6"></path></svg>';

    let sourcesHtml = '';
    if (sources && sources.length > 0) {
        sourcesHtml = `
            <div class="message-sources">
                <div class="sources-label">Sources:</div>
                <div class="sources-list">
                    ${sources.map(s => `
                        <span class="source-tag" title="${escapeHtml(s.snippet)}">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"></path>
                                <polyline points="14 2 14 8 20 8"></polyline>
                            </svg>
                            ${escapeHtml(s.documentName)} (Section ${s.chunkIndex + 1})
                        </span>
                    `).join('')}
                </div>
            </div>
        `;
    }

    messageDiv.innerHTML = `
        <div class="message-avatar">
            ${avatarIcon}
        </div>
        <div class="message-content">
            <div class="message-bubble">${escapeHtml(content)}</div>
            ${sourcesHtml}
        </div>
    `;

    chatMessages.appendChild(messageDiv);
    scrollToBottom();
}

function showTypingIndicator() {
    const indicator = document.createElement('div');
    indicator.className = 'message assistant';
    indicator.id = 'typing-indicator';
    indicator.innerHTML = `
        <div class="message-avatar">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 2a2 2 0 012 2c0 .74-.4 1.39-1 1.73V7h1a7 7 0 017 7h1a1 1 0 011 1v3a1 1 0 01-1 1h-1v1a2 2 0 01-2 2H5a2 2 0 01-2-2v-1H2a1 1 0 01-1-1v-3a1 1 0 011-1h1a7 7 0 017-7h1V5.73c-.6-.34-1-.99-1-1.73a2 2 0 012-2z"></path>
            </svg>
        </div>
        <div class="message-content">
            <div class="typing-indicator">
                <span></span>
                <span></span>
                <span></span>
            </div>
        </div>
    `;
    chatMessages.appendChild(indicator);
    scrollToBottom();
}

function removeTypingIndicator() {
    const indicator = document.getElementById('typing-indicator');
    if (indicator) {
        indicator.remove();
    }
}

function setLoading(loading) {
    isLoading = loading;
    chatInput.disabled = loading;
    sendButton.disabled = loading;
}

function scrollToBottom() {
    chatMessages.scrollTop = chatMessages.scrollHeight;
}

// ============================================
// TOAST NOTIFICATIONS
// ============================================

function showToast(message, type = 'success') {
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;

    const icon = type === 'success'
        ? '<svg class="toast-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 11-5.93-9.14"></path><polyline points="22 4 12 14.01 9 11.01"></polyline></svg>'
        : '<svg class="toast-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line></svg>';

    toast.innerHTML = `
        ${icon}
        <span class="toast-message">${escapeHtml(message)}</span>
        <button class="toast-close" onclick="this.parentElement.remove()">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="18" y1="6" x2="6" y2="18"></line>
                <line x1="6" y1="6" x2="18" y2="18"></line>
            </svg>
        </button>
    `;

    toastContainer.appendChild(toast);

    // Auto remove after 5 seconds
    setTimeout(() => {
        toast.style.animation = 'toastIn 0.3s ease reverse';
        setTimeout(() => toast.remove(), 300);
    }, 5000);
}

// ============================================
// UTILITIES
// ============================================

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// Make deleteDocument available globally
window.deleteDocument = deleteDocument;
