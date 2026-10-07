import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { AppRoutes } from '../../App';
import { renderWithProviders } from '../../test/render';
import { server } from '../../test/server';

const REGISTER_URL = '*/api/v1/auth/register';

function renderSignup() {
  const user = userEvent.setup();
  renderWithProviders(<AppRoutes />, { route: '/signup' });
  return user;
}

async function fillAndSubmit(
  user: ReturnType<typeof userEvent.setup>,
  { name = 'Ada Lovelace', email = 'ada@example.com', password = 'correct horse' } = {},
) {
  if (name) await user.type(screen.getByLabelText('Name'), name);
  if (email) await user.type(screen.getByLabelText('Email'), email);
  if (password) await user.type(screen.getByLabelText('Password'), password);
  await user.click(screen.getByRole('button', { name: 'Create account' }));
}

describe('SignupPage', () => {
  it('shows inline errors and sends nothing when required fields are empty', async () => {
    let requests = 0;
    server.use(http.post(REGISTER_URL, () => void requests++));
    const user = renderSignup();

    await user.click(screen.getByRole('button', { name: 'Create account' }));

    expect(screen.getByLabelText('Name')).toHaveAccessibleDescription('Name is required');
    expect(screen.getByLabelText('Email')).toHaveAccessibleDescription('Email is required');
    expect(screen.getByLabelText('Password')).toHaveAccessibleDescription(
      'Password must be at least 8 characters',
    );
    expect(screen.getByLabelText('Name')).toHaveAttribute('aria-invalid', 'true');
    expect(requests).toBe(0);
  });

  it('validates email format and password length before submitting', async () => {
    const user = renderSignup();

    await fillAndSubmit(user, { email: 'not-an-email', password: '1234567' });

    expect(screen.getByLabelText('Email')).toHaveAccessibleDescription(
      'Enter a valid email address',
    );
    expect(screen.getByLabelText('Password')).toHaveAccessibleDescription(
      'Password must be at least 8 characters',
    );
    expect(screen.getByLabelText('Name')).not.toHaveAttribute('aria-invalid', 'true');
  });

  it('clears a field error once the field is edited', async () => {
    const user = renderSignup();
    await user.click(screen.getByRole('button', { name: 'Create account' }));

    await user.type(screen.getByLabelText('Name'), 'Ada');

    expect(screen.getByLabelText('Name')).not.toHaveAccessibleDescription();
  });

  it('registers and redirects to the login page with a confirmation', async () => {
    let body: unknown;
    server.use(
      http.post(REGISTER_URL, async ({ request }) => {
        body = await request.json();
        return HttpResponse.json(
          {
            id: 1,
            name: 'Ada Lovelace',
            email: 'ada@example.com',
            avatarUrl: null,
            systemRole: 'USER',
            active: true,
            createdAt: '2026-10-06T00:00:00Z',
          },
          { status: 201 },
        );
      }),
    );
    const user = renderSignup();

    await fillAndSubmit(user);

    expect(await screen.findByRole('heading', { level: 1, name: 'Log in' })).toBeInTheDocument();
    expect(screen.getByRole('status')).toHaveTextContent('Account created. Log in to continue.');
    expect(body).toEqual({
      name: 'Ada Lovelace',
      email: 'ada@example.com',
      password: 'correct horse',
    });
  });

  it('shows the duplicate email error from the server on the email field', async () => {
    server.use(
      http.post(REGISTER_URL, () =>
        HttpResponse.json(
          {
            status: 409,
            title: 'Conflict',
            detail: 'An account with this email already exists',
            errors: { email: 'An account with this email already exists' },
          },
          { status: 409, headers: { 'Content-Type': 'application/problem+json' } },
        ),
      ),
    );
    const user = renderSignup();

    await fillAndSubmit(user);

    expect(
      await screen.findByText('An account with this email already exists'),
    ).toBeInTheDocument();
    expect(screen.getByLabelText('Email')).toHaveAccessibleDescription(
      'An account with this email already exists',
    );
    expect(screen.getByRole('heading', { level: 1, name: 'Sign up' })).toBeInTheDocument();
  });

  it('shows server validation errors on their fields', async () => {
    server.use(
      http.post(REGISTER_URL, () =>
        HttpResponse.json(
          { status: 400, title: 'Bad Request', errors: { password: 'must be at most 72 bytes' } },
          { status: 400, headers: { 'Content-Type': 'application/problem+json' } },
        ),
      ),
    );
    const user = renderSignup();

    await fillAndSubmit(user);

    expect(await screen.findByText('Must be at most 72 bytes')).toBeInTheDocument();
    expect(screen.getByLabelText('Password')).toHaveAccessibleDescription(
      'Must be at most 72 bytes',
    );
  });

  it('shows a general error when the server fails unexpectedly', async () => {
    server.use(http.post(REGISTER_URL, () => HttpResponse.json({ status: 500 }, { status: 500 })));
    const user = renderSignup();

    await fillAndSubmit(user);

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Something went wrong. Please try again.',
    );
  });
});
