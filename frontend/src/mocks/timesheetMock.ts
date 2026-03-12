/**
 * Mock da 2ª quinzena para preencher a Grade de Ponto (teste sem backend).
 * Dias 16, 17, 19-23, 25-28 com horários de exemplo.
 */
export type MockGradeByDate = Record<string, string[]>

function pad(n: number) {
  return String(n).padStart(2, '0')
}

/** Retorna gradeByDate mock para o mês/ano (2ª quinzena). */
export function getMockGradeByDate(month: number, year: number): MockGradeByDate {
  const m = pad(month)
  const grade: MockGradeByDate = {
    [`${year}-${m}-16`]: ['14:00', '18:01', '19:00', '21:53'],
    [`${year}-${m}-17`]: ['14:00', '21:19'],
    [`${year}-${m}-19`]: ['13:58', '18:03', '19:04', '21:55'],
    [`${year}-${m}-20`]: ['13:59', '18:00', '19:03', '21:42'],
    [`${year}-${m}-21`]: ['14:00', '17:59', '19:00', '21:42'],
    [`${year}-${m}-22`]: ['14:00', '18:02', '19:05', '21:42'],
    [`${year}-${m}-23`]: ['14:00'],
    [`${year}-${m}-25`]: ['13:59', '18:08', '19:09'],
    [`${year}-${m}-26`]: ['13:59', '18:05', '19:08'],
    [`${year}-${m}-27`]: ['14:00', '17:59', '19:01', '21:42'],
    [`${year}-${m}-28`]: ['14:00', '18:12', '19:14', '21:42'],
  }
  return grade
}

export const MOCK_GRADE_DESCRIPTION = '2ª quinzena (dias 16–28) — apenas na tela, não salva no servidor.'
