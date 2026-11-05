/** One-line tip for each portal route — what this screen is for. */
export function tipForPath(pathname: string): string {
  const exact: Record<string, string> = {
    '/': 'Public marketing site for Zindigi Corporate',
    '/getting-started': 'How merchants start onboarding',
    '/signup': 'Create a new merchant application',
    '/login': 'Sign in with your portal email and password',
    '/verify-otp': 'Enter the email code to continue onboarding',
    '/change-password': 'Set a new password before using the portal',
    '/dashboard': 'Merchant overview of balances, status, and shortcuts',
    '/onboarding': 'Upload entity documents and track KYC for your application',
    '/profile': 'View your account details and party profile',
    '/balance': 'Live wallet balance for the signed-in merchant',
    '/statement': 'Transaction statement for this corporate wallet',
    '/transactions': 'Commission percentage setup for your network',
    '/cards': 'Corporate card products linked to this account',
    '/transfers': 'Start a local, IBFT, UBP, or Raast transfer',
    '/beneficiaries': 'Saved transfer beneficiaries',
    '/invites': 'Invite partners to join this corporate account',
    '/franchises': 'Franchise / sub-merchant network already onboarded',
    '/employees': 'Bulk employee wallet onboarding',
    '/approvals': 'Maker-checker approvals for transfers and requests',
    '/portal-users': 'Assign portal roles for people in your corporate',
    '/admin': 'Back-office queue: review merchant KYC and approve entities',
    '/admin/financing': 'Configure financing rates, collection wallet, and product categories',
    '/financing/desk': 'Operations and checker decide EV bike financing cases',
    '/financing/products': 'Add bikes with price and images for your dealership',
    '/financing/requests': 'Merchant view of financing applications (CNIC and mobile masked)',
  };
  if (exact[pathname]) return exact[pathname];
  if (pathname.startsWith('/admin/review/')) return 'Open a merchant case to check documents and approve or reject';
  if (pathname.startsWith('/transfers/')) return 'Complete this transfer flow with the live DFS wallet';
  if (pathname.startsWith('/partner-kyc/')) return 'Partner KYC capture for an invited person';
  if (pathname.startsWith('/franchise-onboard')) return 'Franchise onboarding form';
  return 'Zindigi Corporate portal';
}
