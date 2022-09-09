//Aarjavi Dharaiya - ard22@njit.edu

import com.amazonaws.regions.Regions;
import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.model.DetectLabelsResult;
import com.amazonaws.services.rekognition.model.DetectLabelsRequest;
import com.amazonaws.services.rekognition.model.Label;
import com.amazonaws.services.rekognition.model.S3Object;
import com.amazonaws.services.rekognition.model.Image;
import java.util.UUID;
import java.util.List;


public class Recogcar {
	
	static AmazonSQS sqs_client;
	static Regions reg_name = Regions.US_EAST_1;
	static AWSCredentials my_credential = null;
	static String s3_img_bucket = "njit-cs-643";
	static String GRP_ID = "grp" + UUID.randomUUID();
	
	public static void main(String[] args) {

		try {
			my_credential = new ProfileCredentialsProvider("default").getCredentials();
		} catch (Exception e) {
			throw new AmazonClientException("Incoorect or missing credential file at /Users/aarjavi/.aws/credentials", e);
		}

		try {
			AmazonRekognition aws_rekognition = AmazonRekognitionClientBuilder.standard().withRegion(reg_name).withCredentials(new AWSStaticCredentialsProvider(my_credential)).build();
			sqs_client = AmazonSQSClientBuilder.standard().withRegion(reg_name).withCredentials(new AWSStaticCredentialsProvider(my_credential)).build();
			String sqs_que_url = sqs_client.listQueues().getQueueUrls().get(0);
			System.out.println("Starting Car Recognition at EC2 instance \nSqs FIFO queue ::  " + sqs_que_url + "\nReading images from s3 bucket:- " + s3_img_bucket + "\n");
		
			for (int img_counter = 1; img_counter<11; img_counter++) {
				String image_file_name = img_counter + ".jpg";
				DetectLabelsRequest detectLabelreq = new DetectLabelsRequest().withMinConfidence(80F).withMaxLabels(20).withImage(new Image().withS3Object(new S3Object().withBucket(s3_img_bucket).withName(image_file_name)));
				DetectLabelsResult detectLabelres = aws_rekognition.detectLabels(detectLabelreq);
				List<Label> labelList = detectLabelres.getLabels();

				for (Label this_label : labelList) {
					if (this_label.getName().equals("Car") ) {
						if (this_label.getConfidence() > 80) {
						Thread.sleep(10000);
						System.out.println("Car detected in the image:-  " + image_file_name  + " with confidence value of:- " + this_label.getConfidence());	
						System.out.println("Pushing it to the queue\n");
						SendMessageRequest toSendMessage = new SendMessageRequest().withQueueUrl(sqs_que_url).withMessageBody(image_file_name) .withMessageGroupId(GRP_ID);
						//.withMessageDeduplicationId(message+ UUID.randomUUID())
						sqs_client.sendMessage(toSendMessage);
						
						//Thread.sleep(5000);
						//Thread.sleep(50000);
						}
					}
				}
			}
			
			Thread.sleep(5000);
			System.out.println("---Done processing all 10 images from S3 bucket.\n---Pushing -1 to the queue.");
			SendMessageRequest toSendMessage = new SendMessageRequest().withQueueUrl(sqs_que_url).withMessageBody("-1").withMessageGroupId(GRP_ID);
			sqs_client.sendMessage(toSendMessage);
			
			System.out.println("*** Car Recoginiton Process Terminated ***");
		} catch (Exception e) {
			System.out.println(e);
		}	
	}	
}