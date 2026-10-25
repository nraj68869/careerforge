export default function MatchPill({ match }) {
  if (!match) return null;
  const tier = match.score >= 70 ? "high" : match.score >= 40 ? "mid" : "low";
  const title = match.missingSkills.length
    ? `Missing: ${match.missingSkills.join(", ")}`
    : "Has every required skill";
  return <span className={`match-pill ${tier}`} title={title}>{match.score}% match</span>;
}