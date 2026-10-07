import { isAxiosError } from 'axios';
import type { ProblemDetail } from './types';

/** Returns the ProblemDetail body of a failed API call, or undefined for network and non-API errors. */
export function getProblem(error: unknown): ProblemDetail | undefined {
  if (
    isAxiosError<ProblemDetail>(error) &&
    error.response?.data &&
    typeof error.response.data === 'object'
  ) {
    return error.response.data;
  }
  return undefined;
}
