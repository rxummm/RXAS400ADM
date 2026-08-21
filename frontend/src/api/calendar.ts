import request from './request'

export interface CalendarEvent {
  id?: number
  title: string
  description?: string
  eventDate: string
  startTime?: string | null
  endTime?: string | null
  eventType?: string
  priority?: number
  color?: string
  isAllDay?: number
  status?: number
}

export const fetchMonthEvents = (year: number, month: number) =>
  request.get('/calendar/events/month', { params: { year, month } })

export const createEvent = (data: Partial<CalendarEvent>) =>
  request.post('/calendar/events', data)

export const updateEvent = (id: number, data: Partial<CalendarEvent>) =>
  request.put(`/calendar/events/${id}`, data)

export const deleteEvent = (id: number) => request.delete(`/calendar/events/${id}`)
