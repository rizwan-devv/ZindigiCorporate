import { Link, NavLink, Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { tipForPath } from '../lib/portalTips';
import { BrandLogo } from './BrandLogo';
import { Footer } from './Footer';
import { PageTip } from './Tip';
import { ThemeToggle } from './ThemeToggle';
import { TransfersNavGroup } from './TransfersNavGroup';

const PUBLIC_PATHS = new Set([
  '/',
  '/getting-started',
  '/signup',
  '/login',
  '/franchise-onboard',
  '/verify-otp',
  '/change-password',
]);

const DESIGN_PORTAL_PATHS = new Set([
  '/balance',
  '/statement',
  '/transactions',
  '/cards',
  '/transfers',
  '/beneficiaries',
  '/invites',
  '/franchises',
]);

function isPublicPath(pathname: string) {
  if (PUBLIC_PATHS.has(pathname)) return true;
  if (pathname.startsWith('/partner-kyc/')) return true;
  return false;
}

function isDesignPortalPath(pathname: string) {
  if (DESIGN_PORTAL_PATHS.has(pathname)) return true;
  if (pathname.startsWith('/transfers/')) return true;
  return false;
}

export function Layout() {
  const { session, logout } = useAuth();
  const location = useLocation();
  const opsMode = location.pathname.startsWith('/admin') && session?.role === 'PLATFORM_ADMIN';
  const isMerchant = !!session && session.role !== 'PLATFORM_ADMIN';
  const isAdmin = session?.role === 'PLATFORM_ADMIN';
  const canStartOnboarding = !session;
  const designPreview = !session && isDesignPortalPath(location.pathname);
  const portalMode = (!!session && !isPublicPath(location.pathname)) || designPreview;
  const showMerchantFinance = (isMerchant && session?.partyStatus === 'ACTIVE') || designPreview;

  if (
    session
    && session.role !== 'PLATFORM_ADMIN'
    && session.firstLogin
    && location.pathname !== '/change-password'
  ) {
    return <Navigate to="/change-password" replace />;
  }
  const showMerchantOps =
    isMerchant && session?.partyStatus === 'ACTIVE' && session?.partyType === 'MERCHANT';
  const financingDesk = !!session?.portalRoles?.some((role) => role === 'FINANCING_OPS' || role === 'FINANCING_CHECKER');

  return (
    <div
      className={[
        'app-shell',
        opsMode ? 'ops-mode' : '',
        portalMode ? 'portal-mode' : 'marketing-mode',
      ]
        .filter(Boolean)
        .join(' ')}
    >
      {portalMode ? (
        <div className="portal-frame">
          <aside className="portal-sidebar">
            <Link to={isAdmin ? '/admin' : designPreview ? '/cards' : '/dashboard'} className="brand portal-brand">
              <BrandLogo
                variant="sidebar"
                title={opsMode ? 'Backoffice' : 'Corporate'}
                subtitle={designPreview ? 'Design Preview' : ''}
              />
            </Link>

            <nav className="portal-nav" aria-label="Portal">
              {designPreview && (
                <>
                  <p className="portal-nav-label">Design tour</p>
                  <NavLink to="/balance" className="portal-link">
                    <span className="portal-link-icon">◉</span>
                    Balance
                  </NavLink>
                  <NavLink to="/statement" className="portal-link">
                    <span className="portal-link-icon">☰</span>
                    Statement
                  </NavLink>
                  <NavLink to="/transactions" className="portal-link">
                    <span className="portal-link-icon">⇄</span>
                    Commission %
                  </NavLink>
                  <NavLink to="/cards" className="portal-link">
                    <span className="portal-link-icon">▭</span>
                    Cards
                  </NavLink>
                  <TransfersNavGroup />
                </>
              )}
              {isAdmin && (
                <>
                  <p className="portal-nav-label">Administration</p>
                  <NavLink to="/admin" className="portal-link" title="Review merchant onboarding and KYC">
                    <span className="portal-link-icon">◈</span>
                    Dashboard
                  </NavLink>
                  <NavLink to="/admin/financing" className="portal-link" title="Configure rates, collection wallet, and product visibility">
                    <span className="portal-link-icon">％</span>
                    Financing setup
                  </NavLink>
                </>
              )}
              {financingDesk && (
                <>
                  {!isAdmin && <p className="portal-nav-label">Financing</p>}
                  <NavLink to="/financing/desk" className="portal-link" title="Review and decide financing applications">
                    <span className="portal-link-icon">✓</span>
                    Loan desk
                  </NavLink>
                </>
              )}
              {isMerchant && (
                <>
                  <p className="portal-nav-label">Overview</p>
                  <NavLink to="/dashboard" className="portal-link" title="Merchant overview and shortcuts">
                    <span className="portal-link-icon">▣</span>
                    Dashboard
                  </NavLink>
                  {session?.partyStatus !== 'ACTIVE' && (
                    <NavLink to="/onboarding" className="portal-link" title="Continue entity documents and KYC">
                      <span className="portal-link-icon">◇</span>
                      My Application
                    </NavLink>
                  )}
                  {showMerchantFinance && !designPreview && (
                    <>
                      <p className="portal-nav-label">Finance</p>
                      <NavLink to="/balance" className="portal-link" title="Check the corporate wallet balance">
                        <span className="portal-link-icon">◉</span>
                        Balance
                      </NavLink>
                      <NavLink to="/statement" className="portal-link" title="Open the wallet statement">
                        <span className="portal-link-icon">☰</span>
                        Statement
                      </NavLink>
                      <NavLink to="/transactions" className="portal-link" title="Set commission percentages">
                        <span className="portal-link-icon">⇄</span>
                        Commission %
                      </NavLink>
                      <NavLink to="/cards" className="portal-link" title="View corporate cards">
                        <span className="portal-link-icon">▭</span>
                        Cards
                      </NavLink>
                      <NavLink to="/financing/products" className="portal-link" title="Add bikes with prices and images for your dealership">
                        <span className="portal-link-icon">▣</span>
                        Products
                      </NavLink>
                      <NavLink to="/financing/requests" className="portal-link" title="See financing cases for your customers">
                        <span className="portal-link-icon">☰</span>
                        Financing requests
                      </NavLink>
                      <TransfersNavGroup />
                    </>
                  )}
                  {showMerchantOps && (
                    <>
                      <p className="portal-nav-label">Network</p>
                      <NavLink to="/invites" className="portal-link" title="Invite partners into this corporate">
                        <span className="portal-link-icon">✉</span>
                        Invites
                      </NavLink>
                      <NavLink to="/franchises" className="portal-link" title="See onboarded franchisees">
                        <span className="portal-link-icon">▣</span>
                        Onboarded
                      </NavLink>
                      <NavLink to="/employees" className="portal-link" title="Bulk-create employee wallets">
                        <span className="portal-link-icon">☷</span>
                        Employees
                      </NavLink>
                      <p className="portal-nav-label">Operations</p>
                      <NavLink to="/approvals" className="portal-link" title="Approve pending maker-checker requests">
                        <span className="portal-link-icon">✓</span>
                        Approvals
                      </NavLink>
                      <NavLink to="/portal-users" className="portal-link" title="Manage portal users and roles">
                        <span className="portal-link-icon">◎</span>
                        Users
                      </NavLink>
                    </>
                  )}
                </>
              )}
              <p className="portal-nav-label">Account</p>
              {session ? (
                <NavLink to="/profile" className="portal-link" title="Open your profile">
                  <span className="portal-link-icon">○</span>
                  Profile
                </NavLink>
              ) : (
                <NavLink to="/login" className="portal-link" title="Sign in to the portal">
                  <span className="portal-link-icon">→</span>
                  Login
                </NavLink>
              )}
              <NavLink to="/" className="portal-link" end title="Leave the portal for the public site">
                <span className="portal-link-icon">⌂</span>
                Marketing site
              </NavLink>
            </nav>

            <div className="portal-sidebar-foot">
              <ThemeToggle />
            </div>
          </aside>

          <div className="portal-main-wrap">
            <header className="portal-topbar">
              <div className="portal-topbar-left">
                <span className="portal-path-chip" title={location.pathname}>{location.pathname}</span>
                <PageTip text={tipForPath(location.pathname)} />
              </div>
              <div className="portal-topbar-right">
                <ThemeToggle className="theme-toggle--compact" />
                {session ? (
                  <>
                    <Link to="/profile" className="portal-user-chip" title="Open profile">
                      <strong>{session.fullName || session.role}</strong>
                      <span>{session.email || roleLabel(session)}</span>
                    </Link>
                    <button className="btn btn-ghost btn-sm" type="button" onClick={logout}>
                      Logout
                    </button>
                  </>
                ) : (
                  <Link className="btn btn-primary btn-sm" to="/login">
                    Login
                  </Link>
                )}
              </div>
            </header>

            <main className={`app-main portal-main ${opsMode ? 'ops-shell' : ''}`}>
              <div className={opsMode ? 'ops-shell' : 'portal-container'}>
                <Outlet />
              </div>
            </main>
          </div>
        </div>
      ) : (
        <>
          <header className="nav">
            <div className="nav-inner container">
              <Link to="/" className="brand">
                <BrandLogo variant="lockup" />
              </Link>
              <nav className="nav-links" aria-label="Main">
                <NavLink to="/" end>
                  Home
                </NavLink>
                <NavLink to="/getting-started">Getting Started</NavLink>
                {canStartOnboarding && <NavLink to="/signup">Onboard</NavLink>}
                {isAdmin && <NavLink to="/admin">Dashboard</NavLink>}
                {isMerchant && <NavLink to="/dashboard">Dashboard</NavLink>}
                {session && <NavLink to="/profile">Profile</NavLink>}
                {!session && (
                  <NavLink to="/cards">Design preview</NavLink>
                )}
              </nav>
              <div className="nav-actions">
                <ThemeToggle />
                {session ? (
                  <>
                    <Link to="/profile" className="portal-user-chip portal-user-chip--nav" title="Open profile">
                      <strong>{session.fullName || session.role}</strong>
                      <span>{session.email || roleLabel(session)}</span>
                    </Link>
                    <button className="btn btn-ghost btn-sm" type="button" onClick={logout}>
                      Logout
                    </button>
                  </>
                ) : (
                  <Link className="btn btn-primary btn-sm" to="/login">
                    Login
                  </Link>
                )}
              </div>
            </div>
            <nav className="nav-mobile container" aria-label="Mobile">
              <NavLink to="/" end>
                Home
              </NavLink>
              <NavLink to="/getting-started">Start</NavLink>
              {canStartOnboarding && <NavLink to="/signup">Onboard</NavLink>}
              {!session && <NavLink to="/cards">Cards</NavLink>}
              {isAdmin && <NavLink to="/admin">Dashboard</NavLink>}
              {isMerchant && <NavLink to="/dashboard">Dashboard</NavLink>}
              {session && <NavLink to="/profile">Profile</NavLink>}
            </nav>
          </header>

          <main className="app-main">
            <Outlet />
          </main>
          <Footer />
        </>
      )}
    </div>
  );
}

function roleLabel(session: { role: string; portalRoles?: string[] }) {
  if (session.role === 'PLATFORM_ADMIN') return 'Platform admin';
  const roles = session.portalRoles || [];
  if (roles.includes('FINANCING_CHECKER')) return 'Financing checker';
  if (roles.includes('FINANCING_OPS')) return 'Financing operations';
  return session.role.replaceAll('_', ' ');
}
