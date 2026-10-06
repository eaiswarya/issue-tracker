import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { server } from '../test/server';
import { apiClient } from './client';

describe('apiClient', () => {
  it('sends requests under the configured API base URL', async () => {
    server.use(http.get('*/api/v1/ping', () => HttpResponse.json({ pong: true })));

    const response = await apiClient.get('/ping');

    expect(apiClient.defaults.baseURL).toBe('/api/v1');
    expect(response.data).toEqual({ pong: true });
  });

  it('rejects on error responses', async () => {
    server.use(
      http.get('*/api/v1/broken', () => HttpResponse.json({ status: 500 }, { status: 500 })),
    );

    await expect(apiClient.get('/broken')).rejects.toMatchObject({ response: { status: 500 } });
  });
});
