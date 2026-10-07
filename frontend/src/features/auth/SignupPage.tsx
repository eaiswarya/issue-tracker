import { useState, type ChangeEvent, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router';
import { useRegister } from '../../api/auth';
import { getProblem } from '../../api/errors';
import type { RegisterRequest } from '../../api/types';

type Field = keyof RegisterRequest;
type FieldErrors = Partial<Record<Field, string>>;

const FIELDS: { name: Field; label: string; type: string; autoComplete: string }[] = [
  { name: 'name', label: 'Name', type: 'text', autoComplete: 'name' },
  { name: 'email', label: 'Email', type: 'email', autoComplete: 'email' },
  { name: 'password', label: 'Password', type: 'password', autoComplete: 'new-password' },
];

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+$/;
const MIN_PASSWORD_LENGTH = 8;

function validate(values: RegisterRequest): FieldErrors {
  const errors: FieldErrors = {};
  if (!values.name.trim()) {
    errors.name = 'Name is required';
  }
  if (!values.email.trim()) {
    errors.email = 'Email is required';
  } else if (!EMAIL_PATTERN.test(values.email.trim())) {
    errors.email = 'Enter a valid email address';
  }
  if (values.password.length < MIN_PASSWORD_LENGTH) {
    errors.password = `Password must be at least ${MIN_PASSWORD_LENGTH} characters`;
  }
  return errors;
}

function capitalise(message: string) {
  return message.charAt(0).toUpperCase() + message.slice(1);
}

export function SignupPage() {
  const navigate = useNavigate();
  const register = useRegister();
  const [values, setValues] = useState<RegisterRequest>({ name: '', email: '', password: '' });
  const [errors, setErrors] = useState<FieldErrors>({});
  const [formError, setFormError] = useState<string>();

  function handleChange(event: ChangeEvent<HTMLInputElement>) {
    const field = event.target.name as Field;
    setValues((current) => ({ ...current, [field]: event.target.value }));
    setErrors((current) => ({ ...current, [field]: undefined }));
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setFormError(undefined);
    const clientErrors = validate(values);
    setErrors(clientErrors);
    if (Object.keys(clientErrors).length > 0) {
      return;
    }
    register.mutate(
      { name: values.name.trim(), email: values.email.trim(), password: values.password },
      {
        onSuccess: () => navigate('/login', { state: { registered: true } }),
        onError: (error) => {
          const serverErrors = getProblem(error)?.errors;
          if (serverErrors && Object.keys(serverErrors).length > 0) {
            setErrors(
              Object.fromEntries(
                Object.entries(serverErrors).map(([field, message]) => [
                  field,
                  capitalise(message),
                ]),
              ),
            );
          } else {
            setFormError('Something went wrong. Please try again.');
          }
        },
      },
    );
  }

  return (
    <section className="mx-auto max-w-sm">
      <h1 className="text-2xl font-semibold">Sign up</h1>
      <form noValidate onSubmit={handleSubmit} className="mt-6 space-y-4">
        {formError && (
          <p role="alert" className="rounded bg-red-50 px-3 py-2 text-sm text-red-700">
            {formError}
          </p>
        )}
        {FIELDS.map((field) => {
          const error = errors[field.name];
          const errorId = `signup-${field.name}-error`;
          return (
            <div key={field.name}>
              <label htmlFor={`signup-${field.name}`} className="block text-sm font-medium">
                {field.label}
              </label>
              <input
                id={`signup-${field.name}`}
                name={field.name}
                type={field.type}
                autoComplete={field.autoComplete}
                value={values[field.name]}
                onChange={handleChange}
                aria-invalid={error ? true : undefined}
                aria-describedby={error ? errorId : undefined}
                className={`mt-1 block w-full rounded border px-3 py-2 ${
                  error ? 'border-red-500' : 'border-slate-300'
                }`}
              />
              {error && (
                <p id={errorId} className="mt-1 text-sm text-red-700">
                  {error}
                </p>
              )}
            </div>
          );
        })}
        <button
          type="submit"
          disabled={register.isPending}
          className="w-full rounded bg-slate-900 px-4 py-2 font-medium text-white hover:bg-slate-700 disabled:opacity-60"
        >
          Create account
        </button>
      </form>
      <p className="mt-4 text-sm text-slate-600">
        Already have an account?{' '}
        <Link to="/login" className="text-blue-700 underline">
          Log in
        </Link>
      </p>
    </section>
  );
}
