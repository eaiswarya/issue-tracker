import { useMutation } from '@tanstack/react-query';
import { apiClient } from './client';
import type { RegisterRequest, User } from './types';

export function useRegister() {
  return useMutation({
    mutationFn: async (request: RegisterRequest) => {
      const response = await apiClient.post<User>('/auth/register', request);
      return response.data;
    },
  });
}
