import { Link, NavLink, Outlet } from 'react-router';

const navLinkClass = ({ isActive }: { isActive: boolean }) =>
  `rounded px-3 py-1.5 text-sm font-medium ${
    isActive ? 'bg-slate-700 text-white' : 'text-slate-300 hover:bg-slate-800 hover:text-white'
  }`;

export function AppShell() {
  return (
    <div className="flex min-h-screen flex-col bg-slate-50 text-slate-900">
      <header className="bg-slate-900">
        <div className="mx-auto flex h-14 max-w-7xl items-center justify-between px-4">
          <Link to="/projects" className="text-lg font-semibold text-white">
            Issue Tracker
          </Link>
          <nav aria-label="Main" className="flex gap-1">
            <NavLink to="/projects" className={navLinkClass}>
              Projects
            </NavLink>
            <NavLink to="/login" className={navLinkClass}>
              Log in
            </NavLink>
          </nav>
        </div>
      </header>
      <main className="mx-auto w-full max-w-7xl flex-1 px-4 py-6">
        <Outlet />
      </main>
    </div>
  );
}
