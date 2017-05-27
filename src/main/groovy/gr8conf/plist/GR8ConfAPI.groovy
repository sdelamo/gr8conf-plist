package gr8conf.plist

import groovy.json.JsonSlurper

class GR8ConfAPI {
    private static final String ROOT_API_URL = 'http://cfp.gr8conf.org'
    private static final String API_VERSION_2 = 'api2'
    private static final String API_AGENDA_PATH = 'agenda'
    private static final String API_SPEAKERS_PATH = 'speakers'
    private static final String API_TALKS_PATH = 'talks'

    def jsonSlurper = new JsonSlurper()
    def tracks = []
    def rooms = []
    def people = []
    def sessions = []

    def extractPresenters = { speakers, allspeakers ->
        def presenters = []
        speakers.each { speaker ->
            def foundSpeaker = allspeakers.find {it.peopleId == speaker.id}
            if(foundSpeaker) {
                presenters << foundSpeaker.twitter
            }
        }
        presenters
    }

    def stripHtml = { html ->
        html = html.replaceAll('<p>','')
        html = html.replaceAll('</p>','')
        html = html.replaceAll('<li>','')
        html = html.replaceAll('</li>','')
        html = html.replaceAll('<ul>','')
        html = html.replaceAll('</ul>','')
        html
    }

    def lastName = { fullname ->
        def tokens = fullname.tokenize(' ')
        def arr = []
        if(tokens.size() > 1) {
            for(int i=1;i<tokens.size();i++) {
                arr << tokens[i]
            }
        }
        arr.join(' ')
    }

    def roomForTalk = { agendaObject, talkId ->
        for(int i = 0; i < agendaObject.size();i++) {
            def day = agendaObject[i].day

            for (def track in agendaObject[i].tracks) {
                for (def slot in track.slots) {
                    if (slot?.talk?.id == talkId) {
                        def roomName = track.room != null && track.room != '' ? track.room : track.name
                        return roomName
                    }
                }
            }
        }
    }

    def trackIdForTalk = { agendaObject, talkId ->
        for(int i = 0; i < agendaObject.size();i++) {
            def day = agendaObject[i].day

            for (def track in agendaObject[i].tracks) {
                for (def slot in track.slots) {
                    if (slot?.talk?.id == talkId) {
                        return track.id
                    }
                }
            }
        }
    }

    def starDateForTalk = { agendaObject, talkId ->
        for(int i = 0; i < agendaObject.size();i++) {
            def day = agendaObject[i].day

            for(def track in agendaObject[i].tracks) {
                for(def slot in track.slots) {
                    if(slot?.talk?.id == talkId) {
                        return Date.parse('yyyy-MM-dd HH:mm', "${day} ${slot.start}")
                    }
                }
            }
        }
        return null
    }


    def calculateRoomId = { rooms, room ->
        for(int i = 0; i < rooms.size();i++) {
            if(rooms[i] == room) {
                return i
            }
        }
    }

    def calculateTrackId = { tracks, trackId ->
        for(int i = 0; i < tracks.size();i++) {
            if(tracks[i].id == trackId) {
                return i
            }
        }
    }

    def extractEventData(eventId) {
        def agendaObject = fetchAgendaObject(eventId)
        extractTracks(agendaObject)
        extractRooms(agendaObject)
        extractPeople(eventId)
        extractSessions(agendaObject, eventId)
    }

    def extractRooms(def object) {
        for(def obj in object) {
            for(def track in obj.tracks) {
                def roomName = track.room != null && track.room.trim() != '' ? track.room : track.name
                rooms << roomName
            }
        }
        rooms = rooms.unique()
    }

    def extractPeople(def eventId) {
        def text = new URL("${ROOT_API_URL}/${API_VERSION_2}/${API_SPEAKERS_PATH}/${eventId}").text

        def object = jsonSlurper.parseText(text)
        people = object.collect { [peopleId: it.id,
                                       first: it.name.tokenize(' ')[0],
                                       last:lastName(it.name),
                                       twitter: it.twitter,
                                       bio: stripHtml(it.bio),
                                       active: true] }
    }

    def fetchAgendaObject(def eventId) {
        def text = new URL("${ROOT_API_URL}/${API_VERSION_2}/${API_AGENDA_PATH}/${eventId}").text
        jsonSlurper.parseText(text)
    }

    def extractTracks(def object) {
        for(def obj in object) {
            for(def track in obj.tracks) {
                tracks << [id:track.id, name: track.name]
            }
        }
        tracks = tracks.unique()
    }

    def extractSessions(def agendaObject, def eventId) {
        def text = new URL("${ROOT_API_URL}/${API_VERSION_2}/${API_TALKS_PATH}/${eventId}").text
        def talksObject = jsonSlurper.parseText(text)
        sessions = talksObject.collect {
            def presenters = extractPresenters(it.speakers, people)
            [
                    active            : true,
                    date              : starDateForTalk(agendaObject, it.id),
                    duration          : it.slot.duration,
                    trackId           : calculateTrackId(tracks, trackIdForTalk(agendaObject, it.id)),
                    column            : 1,
                    sessionNumber     : it.id,
                    title             : it.title,
                    sessionDescription: stripHtml(it.summary),
                    presenters        : presenters,
                    roomId            : calculateRoomId(rooms, roomForTalk(agendaObject, it.id))]
        }
    }
}
