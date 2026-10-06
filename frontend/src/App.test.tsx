import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { AppRoutes } from './App';
import { renderWithProviders } from './test/render';

describe('routes', () => {
  it.each([
    ['/login', 'Log in'],
    ['/signup', 'Sign up'],
    ['/projects', 'Projects'],
  ])('%s renders its placeholder page', (path, heading) => {
    renderWithProviders(<AppRoutes />, { route: path });

    expect(screen.getByRole('heading', { level: 1, name: heading })).toBeInTheDocument();
  });

  it('renders the board for the project key in the URL', () => {
    renderWithProviders(<AppRoutes />, { route: '/projects/ABC/board' });

    expect(screen.getByRole('heading', { level: 1, name: 'ABC board' })).toBeInTheDocument();
  });

  it('redirects / to the projects page', () => {
    renderWithProviders(<AppRoutes />, { route: '/' });

    expect(screen.getByRole('heading', { level: 1, name: 'Projects' })).toBeInTheDocument();
  });

  it('shows a not found page for unknown routes', () => {
    renderWithProviders(<AppRoutes />, { route: '/nope' });

    expect(screen.getByRole('heading', { level: 1, name: 'Page not found' })).toBeInTheDocument();
  });
});

describe('app shell', () => {
  it('renders a top bar and the route content in the main area', () => {
    renderWithProviders(<AppRoutes />, { route: '/projects' });

    const banner = screen.getByRole('banner');
    expect(banner).toHaveTextContent('Issue Tracker');
    expect(screen.getByRole('main')).toContainElement(
      screen.getByRole('heading', { level: 1, name: 'Projects' }),
    );
  });

  it('is rendered by the default export with its own providers', async () => {
    const { default: App } = await import('./App');
    window.history.pushState({}, '', '/signup');

    render(<App />);

    expect(screen.getByRole('heading', { level: 1, name: 'Sign up' })).toBeInTheDocument();
  });
});
