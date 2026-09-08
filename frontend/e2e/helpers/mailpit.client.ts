export interface MailpitMessage {
  ID: string;
  From: { Name: string; Address: string };
  To: { Name: string; Address: string }[];
  Subject: string;
  Created: string;
  Snippet: string;
}

export interface MailpitSearchResponse {
  total: number;
  unread: number;
  count: number;
  messages: MailpitMessage[];
}

export interface MailpitMessageDetail extends MailpitMessage {
  Text: string;
  HTML: string;
}

export class MailpitClient {
  private baseUrl: string;

  constructor(baseUrl = process.env['MAILPIT_URL'] || 'http://localhost:8025') {
    this.baseUrl = baseUrl;
  }

  /**
   * Delete all stored messages in Mailpit to ensure clean test state
   */
  async deleteAllMessages(): Promise<void> {
    try {
      const response = await fetch(`${this.baseUrl}/api/v1/messages`, {
        method: 'DELETE',
      });
      if (!response.ok) {
        throw new Error(`Failed to delete messages: ${response.statusText}`);
      }
    } catch (error) {
      console.warn('Mailpit deleteAllMessages failed (server might be starting):', error);
    }
  }

  /**
   * Search messages by recipient or query string
   */
  async searchMessages(query: string): Promise<MailpitMessage[]> {
    const url = `${this.baseUrl}/api/v1/search?query=${encodeURIComponent(query)}`;
    const response = await fetch(url);
    if (!response.ok) {
      throw new Error(`Mailpit search failed: ${response.statusText}`);
    }
    const data: MailpitSearchResponse = await response.json();
    return data.messages || [];
  }

  /**
   * Fetch full message content including HTML and Text body by Message ID
   */
  async getMessageDetail(messageId: string): Promise<MailpitMessageDetail> {
    const url = `${this.baseUrl}/api/v1/message/${messageId}`;
    const response = await fetch(url);
    if (!response.ok) {
      throw new Error(`Failed to fetch message detail for ID ${messageId}`);
    }
    return response.json();
  }

  /**
   * Poll Mailpit until a matching email arrives or timeout occurs
   */
  async waitForEmail(
    recipientEmail: string,
    subjectSnippet: string,
    timeoutMs = 15000,
    pollIntervalMs = 1000
  ): Promise<MailpitMessageDetail> {
    const startTime = Date.now();
    const query = `to:${recipientEmail}`;

    while (Date.now() - startTime < timeoutMs) {
      const messages = await this.searchMessages(query);
      const match = messages.find((m) =>
        m.Subject.toLowerCase().includes(subjectSnippet.toLowerCase())
      );

      if (match) {
        return await this.getMessageDetail(match.ID);
      }

      await new Promise((resolve) => setTimeout(resolve, pollIntervalMs));
    }

    throw new Error(
      `Timeout waiting for email to ${recipientEmail} with subject containing "${subjectSnippet}"`
    );
  }

  /**
   * Helper to extract activation URL or code from HTML email body
   */
  extractLinkFromHtml(html: string, linkPattern: RegExp): string | null {
    const match = html.match(linkPattern);
    return match ? match[1] || match[0] : null;
  }
}
