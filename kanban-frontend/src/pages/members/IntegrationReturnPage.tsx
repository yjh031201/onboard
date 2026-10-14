import { Navigate, useLocation } from "react-router-dom";
import { getIntegrationReturnPath } from "../../lib/integrations";

/**
 * 연동(OAuth) 콜백 착지 페이지. 백엔드는 /members?integration=...&status=...로 돌려보내는데
 * 팀원 페이지는 /projects/{id}/members라서, 연동을 시작한 프로젝트의 팀원 페이지로 쿼리를 그대로 들고 넘어간다
 * (배너 표시는 MembersPage가 한다). 시작한 페이지를 모르면 홈으로 보낸다.
 */
export default function IntegrationReturnPage() {
  const { search } = useLocation();
  const returnPath = getIntegrationReturnPath();

  return <Navigate to={returnPath ? returnPath + search : "/"} replace />;
}
