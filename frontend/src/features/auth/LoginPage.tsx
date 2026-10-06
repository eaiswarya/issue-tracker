import { useLocation } from 'react-router';

export function LoginPage() {
  const location = useLocation();
  const registered = (location.state as { registered?: boolean } | null)?.registered === true;

  return (
    <section>
      <h1 className="text-2xl font-semibold">Log in</h1>
      {registered && (
        <p role="status" className="mt-4 rounded bg-green-50 px-3 py-2 text-sm text-green-800">
          Account created. Log in to continue.
        </p>
      )}
    </section>
  );
}
