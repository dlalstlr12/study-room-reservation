import { Link } from 'react-router-dom'

export function NotFoundPage() {
  return (
    <div className="page page--narrow">
      <div className="page__head">
        <h1>404</h1>
        <p className="page__lead">존재하지 않는 페이지입니다.</p>
      </div>
      <Link to="/" className="btn btn--primary">
        대시보드로
      </Link>
    </div>
  )
}
