import { Wordmark } from '@/components/Wordmark'
import { AccountMenu } from '@/features/account/components/AccountMenu'

export function AppHeader() {
  return (
    <header className="mx-auto flex w-full max-w-6xl items-center justify-between px-6 py-5">
      <Wordmark />
      <AccountMenu />
    </header>
  )
}
