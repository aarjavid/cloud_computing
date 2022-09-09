
 //Aarjavi Dharaiya - ard22@njit.edu 
 
 

import com.amazonaws.regions.Regions;
import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.rekognition.model.DetectTextResult;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.rekognition.model.S3Object;
import com.amazonaws.services.rekognition.model.TextDetection;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import com.amazonaws.services.rekognition.model.AmazonRekognitionException;
import com.amazonaws.services.rekognition.model.DetectTextRequest;
import java.io.FileNotFoundException;
import java.util.List;
import java.io.IOException;
import java.io.FileWriter;  

public class Recogtext {

	
	static Regions reg_name = Regions.US_EAST_1;
	static AWSCredentials my_credential = null;
	static int[] image_index_status = new int[]{ 0,0,0,0,0,0,0,0,0,0}; 
	static String s3_img_bucket = "njit-cs-643";
	static FileWriter bw;

	public static void main(String[] args) throws FileNotFoundException {
		try {
			bw = new FileWriter("output.txt");
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		try {
			my_credential = new ProfileCredentialsProvider("default").getCredentials();
		} catch (Exception e) {
			throw new AmazonClientException("Incoorect or missing credential file at /Users/aarjavi/.aws/credentials", e);
		}
		
		AmazonSQS sqs_client = AmazonSQSClientBuilder.standard().withRegion(reg_name).withCredentials(new AWSStaticCredentialsProvider(my_credential)).build();
		String sqs_que_url = sqs_client.listQueues().getQueueUrls().get(0);
		System.out.println("Starting TextRecognition at EC2 instance \nSqs FIFO queue ::  " + sqs_que_url + "\nReading images from s3 bucket:- " + s3_img_bucket + "\n");

		int program_end = 0;
		String prev_body = "-";
		String body;
		
		while (true) {
			try {
				
				ReceiveMessageRequest recv_msg_req = new ReceiveMessageRequest(sqs_que_url);
				List<Message> recieved_msgs = sqs_client.receiveMessage(recv_msg_req).getMessages();

				for (Message this_msg : recieved_msgs) {
					
					body = this_msg.getBody();
					String msg_handle = this_msg.getReceiptHandle();	
					if (body.equals(prev_body)) {
						//System.out.println("Body:- " + body + "   Prev Body:- " + prev_body);
						//System.out.println("Deleting this message " + msg_handle + "\n");
						sqs_client.deleteMessage(new DeleteMessageRequest(sqs_que_url, msg_handle));
						continue;
					}
					prev_body = body;
					//System.out.println("Debug 1");
					//System.out.println("Recieved a Message");
					//System.out.println("Body:- " + body);
					
					if (body.equals("-1")) {
						program_end = 1;
						//System.out.println(" -1 detected in For Loop");
						//System.out.println("Deleting this message " + msg_handle + "\n");
						sqs_client.deleteMessage(new DeleteMessageRequest(sqs_que_url, msg_handle));
						break;
					}
					
					//System.out.println("Debug 2");
					TextDetectProc(this_msg);
													
					//System.out.println("Deleting this message:- " + msg_handle + "\n");
					sqs_client.deleteMessage(new DeleteMessageRequest(sqs_que_url, msg_handle));
				}
				if (program_end == 1) {
					System.out.println("-1 recieved from Sender\n");
					break;
				}
				
			} catch (Exception e) {
				//Did not receive anything;
			}
		}
	try {
		bw.close();
	} catch (IOException e) {
		e.printStackTrace();
	}	
    System.out.println("*** Text Detection Process Terminated ***");
	}
	
	static void TextDetectProc(Message msg) throws IOException {
		
		try {
		
		String img_ind = msg.getBody().replace(".jpg", "");
		int img_index_int = Integer.parseInt(img_ind);	
		if(image_index_status[img_index_int] == 1) {
			return;
		}
		image_index_status[img_index_int] = 1;
		
		DetectTextRequest detectTextreq = new DetectTextRequest().withImage(new Image().withS3Object(new S3Object().withBucket(s3_img_bucket).withName(msg.getBody() ) ) );	
		AmazonRekognition aws_rekognition = AmazonRekognitionClientBuilder.standard().withRegion(reg_name).withCredentials(new AWSStaticCredentialsProvider(my_credential)).build();
		DetectTextResult detectTextres = aws_rekognition.detectText(detectTextreq);		
		List<TextDetection> textList = detectTextres.getTextDetections();

		
		
		
		boolean noText = textList.isEmpty();
		if(noText) {
			return;
		}
		
		System.out.println("Detected Text in image:- " + msg.getBody() + "\n");
		
		
		for (TextDetection text : textList) {	
			try {
			      
			      bw.write("\nImage_index:- " + msg.getBody() + "   Image_Text:- " + text.getDetectedText());
			      
			    } catch (IOException e) {
			      System.out.println("An error occurred in writing to the file.");
			      e.printStackTrace();
			    }
			
				
			}
		bw.write("\n");
		
		
		} catch (AmazonRekognitionException e) {
			System.out.println("Caught AmazonRekognitionException");
		}

	}
}