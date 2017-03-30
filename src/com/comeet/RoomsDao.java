
package com.comeet;

import com.comeet.data.DataRepository;
import com.comeet.exchange.ExchangeServiceException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import microsoft.exchange.webservices.data.core.ExchangeService;
import microsoft.exchange.webservices.data.core.exception.service.remote.ServiceRequestException;
import microsoft.exchange.webservices.data.core.exception.service.remote.ServiceResponseException;
import microsoft.exchange.webservices.data.core.service.item.Appointment;
import microsoft.exchange.webservices.data.property.complex.EmailAddress;
import microsoft.exchange.webservices.data.property.complex.EmailAddressCollection;
import microsoft.exchange.webservices.data.property.complex.MessageBody;

// BUGBUG: [BOOKAROOM-45] https://bookaroom.atlassian.net/browse/BOOKAROOM-45
// CHECKSTYLE DISABLE: JavadocMethod
public class RoomsDao {

    /**
     * The Exchange service is injected via the constructor.
     */
    protected ExchangeService service;

    /**
     * Constructs a rooms Data Access Object.
     * 
     * @param service The exchange service to access for Room data.
     */
    public RoomsDao(ExchangeService service) {
        this.service = service;
    }

    /**
     * Creates an appointment.
     * 
     * @param start Appointment start time.
     * @param end Appointment end time.
     * @param subject The subject line.
     * @param body Body text for the appointment
     * @param recips Recipient list.
     * @return The meeting(s) created.
     * @throws ServiceResponseException When the exchange service bails.
     * @throws Exception //TODO: For unknown reasons.
     */
    public List<Meeting> makeAppointment(String start, String end, String subject, String body,
                    List<String> recips) throws ServiceResponseException, Exception {

        // ?start=2017-05-23|9:00:00&end=2017-05-23|9:00:00&
        // subject=testSubject&body=testBody&recipients=CambMa1Story305@meetl.ink,jablack@meetl.ink

        Appointment appointment = new Appointment(service);
        appointment.setSubject(subject);
        appointment.setBody(MessageBody.getMessageBodyFromText(body));

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd*HH:mm:ss");
        Date startDate = formatter.parse(start); // "2017-05-23|5:00:00");
        Date endDate = formatter.parse(end); // "2017-05-23|6:00:00");
        appointment.setStart(startDate);
        appointment.setEnd(endDate);

        appointment.setRecurrence(null);

        for (String s : recips) {
            appointment.getRequiredAttendees().add(s);
        }
        // appointment.getRequiredAttendees().add("CambMa1Story305@meetl.ink");
        // appointment.getRequiredAttendees().add("jablack@meetl.ink");

        appointment.save();

        List<Meeting> list = new ArrayList<Meeting>();

        return list;
    }



    /**
     * Gets a list of room email addresses.
     * 
     * @return List of room email addresses.
     * @throws Exception When the service fails to be created.
     */
    public List<EmailAddress> getRoomsList()
                    throws ServiceRequestException, ServiceResponseException, ExchangeServiceException {

        List<EmailAddress> names = new ArrayList<EmailAddress>();

        try {

            EmailAddressCollection c = service.getRoomLists();
            for (EmailAddress e : c) {

                Collection<EmailAddress> rooms = service.getRooms(e);

                for (EmailAddress r : rooms) {
                    System.out.println(r.toString());
                    System.out.println(r.getAddress());
                    System.out.println(r.getName());
                    names.add(r);
                }
            }
        } catch (ServiceRequestException e) {
            throw e;
        } catch (ServiceResponseException e) {
            throw e;
        } catch (Exception e) {
            throw new ExchangeServiceException(e);
        }
        
        return names;
    }

    /**
     * Gets all rooms at the organization.
     * 
     * @return A list of rooms or null on error.
     * @throws ServiceRequestException 
     *          When the exchange service's request was invalid or malformed.
     * @throws ServiceResponseException
     *          When the exchange server's response was invalid or malformed.
     * @throws ExchangeServiceException
     *           When something else went wrong with the service.
     */
    public List<Room> getAllRooms() throws ServiceResponseException, ServiceRequestException, ExchangeServiceException {
        List<EmailAddress> rooms = null;
        List<Room> roomList = null;

        try {
            rooms = getRoomsList();

            File file = new File("Roo.dat");

            file.delete();

            if (!file.exists()) {

                roomList = new ArrayList<Room>();


                for (EmailAddress s : rooms) {
                    Room room = new Room();
                    room.setName(s.getName());
                    room.setEmail(s.getAddress());
                    retrieveMetadata(room);
                    roomList.add(room);
                }

                // User user = new User(1, "Peter", "Teacher");

                // userList.add(user);
                saveRoomList(roomList);
            } else {
                FileInputStream fis = new FileInputStream(file);
                ObjectInputStream ois = new ObjectInputStream(fis);
                roomList = (List<Room>) ois.readObject();
                ois.close();
            }
        } catch (IOException e) {
            // TODO: Auto-generated catch block.
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            // TODO: Auto-generated catch block.
            e.printStackTrace();
        }
        
        return roomList;
    }

    private void saveRoomList(List<Room> roomList) {
        try {
            File file = new File("Roo.dat");
            FileOutputStream fos;
            fos = new FileOutputStream(file);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(roomList);
            oos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void retrieveMetadata(Room room) {
        DataRepository db = new DataRepository();

        Room metadata = db.retrieveRoomMetadata(room.getEmail());

        if (metadata != null) {
            room.setCapacity(metadata.getCapacity());
            room.setCountry(metadata.getCountry());
            room.setBuilding(metadata.getBuilding());
            room.setNavigationMap(metadata.getNavigationMap());
            room.setLatitude(metadata.getLatitude());
            room.setCapacity(metadata.getCapacity());
            room.setLongitude(metadata.getLongitude());
            room.setMetroarea(metadata.getMetroarea());
            room.setState(metadata.getState());
            room.setRoomPic(metadata.getRoomPic());
        }

    }
}
