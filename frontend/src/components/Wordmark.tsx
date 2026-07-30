import { Link } from 'react-router-dom'
import { Logo } from '@/components/Logo'

export function Wordmark() {
  return (
    <Link
      to="/"
      className="flex items-center gap-2 rounded-sm focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-ring"
    >
      <Logo className="size-5" />
      <span className="font-stretch-expanded text-sm font-black tracking-wide">LIFTFORM</span>
    </Link>
  )
}
