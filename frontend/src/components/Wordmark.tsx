import { Link } from 'react-router-dom'

export function Wordmark() {
  return (
    <Link
      to="/"
      className="flex items-center gap-2 rounded-sm focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-ring"
    >
      <span aria-hidden className="size-3 rounded-[2px] bg-plate-red" />
      <span className="font-stretch-expanded text-sm font-black tracking-wide">LIFTFORM</span>
    </Link>
  )
}
