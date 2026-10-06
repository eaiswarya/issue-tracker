export type SystemRole = 'ADMIN' | 'USER';

export interface User {
  id: number;
  name: string;
  email: string;
  avatarUrl: string | null;
  systemRole: SystemRole;
  active: boolean;
  createdAt: string;
}

export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
}

/** RFC 7807 error body returned by the API. `errors` maps field names to messages. */
export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  errors?: Record<string, string>;
}
