import gr8conf.plist.GR8ConfAPI
import gr8conf.plist.PlistGenerator

println 'Executing'

def eventId = 10

def gr8confAPI = new GR8ConfAPI()
gr8confAPI.extractEventData(eventId)

def pListGenerator = new PlistGenerator()
pListGenerator.savePlist(gr8confAPI.tracks, gr8confAPI.rooms, gr8confAPI.people, gr8confAPI.sessions, 'gr8confeu-2017.plist')