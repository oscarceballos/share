package sparql2flink.out;

import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.common.operators.Order;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.core.fs.FileSystem;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import sparql2flink.runner.functions.*;
import sparql2flink.runner.LoadTransformTriples;
import sparql2flink.runner.functions.order.*;
import java.math.*;

public class Query {
	public static void main(String[] args) throws Exception {

		final ParameterTool params = ParameterTool.fromArgs(args);

		if (!params.has("dataset") && !params.has("output")) {
			System.out.println("Use --dataset to specify dataset path and use --output to specify output path.");
		}

		//************ Environment (DataSet) and Source (static RDF dataset) ************
		final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();
		DataSet<Triple> dataset = LoadTransformTriples.loadTriplesFromDataset(env, params.get("dataset"));

		//************ Applying Transformations ************
		DataSet<SolutionMapping> sm1 = dataset
			.filter(new T2T_FF(null, "http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/vocabulary/reviewFor", "http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/instances/dataFromProducer1/Product12"))
			.map(new T2SM_MF("?review", null, null));

		DataSet<SolutionMapping> sm2 = dataset
			.filter(new T2T_FF(null, "http://purl.org/dc/elements/1.1/title", null))
			.map(new T2SM_MF("?review", null, "?title"));

		DataSet<SolutionMapping> sm3 = sm1.join(sm2)
			.where(new SM_JKS(new String[]{"?review"}))
			.equalTo(new SM_JKS(new String[]{"?review"}))
			.with(new SM_JF());

		DataSet<SolutionMapping> sm4 = dataset
			.filter(new T2T_FF(null, "http://purl.org/stuff/rev#text", null))
			.map(new T2SM_MF("?review", null, "?text"));

		DataSet<SolutionMapping> sm5 = sm3.join(sm4)
			.where(new SM_JKS(new String[]{"?review"}))
			.equalTo(new SM_JKS(new String[]{"?review"}))
			.with(new SM_JF());

		DataSet<SolutionMapping> sm6 = dataset
			.filter(new T2T_FF(null, "http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/vocabulary/reviewDate", null))
			.map(new T2SM_MF("?review", null, "?reviewDate"));

		DataSet<SolutionMapping> sm7 = sm5.join(sm6)
			.where(new SM_JKS(new String[]{"?review"}))
			.equalTo(new SM_JKS(new String[]{"?review"}))
			.with(new SM_JF());

		DataSet<SolutionMapping> sm8 = dataset
			.filter(new T2T_FF(null, "http://purl.org/stuff/rev#reviewer", null))
			.map(new T2SM_MF("?review", null, "?reviewer"));

		DataSet<SolutionMapping> sm9 = sm7.join(sm8)
			.where(new SM_JKS(new String[]{"?review"}))
			.equalTo(new SM_JKS(new String[]{"?review"}))
			.with(new SM_JF());

		DataSet<SolutionMapping> sm10 = dataset
			.filter(new T2T_FF(null, "http://xmlns.com/foaf/0.1/name", null))
			.map(new T2SM_MF("?reviewer", null, "?reviewerName"));

		DataSet<SolutionMapping> sm11 = sm9.join(sm10)
			.where(new SM_JKS(new String[]{"?reviewer"}))
			.equalTo(new SM_JKS(new String[]{"?reviewer"}))
			.with(new SM_JF());

		DataSet<SolutionMapping> sm12 = dataset
			.filter(new T2T_FF(null, "http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/vocabulary/rating1", null))
			.map(new T2SM_MF("?review", null, "?rating1"));

		DataSet<SolutionMapping> sm13 = sm11.leftOuterJoin(sm12)
			.where(new SM_JKS(new String[]{"?review"}))
			.equalTo(new SM_JKS(new String[]{"?review"}))
			.with(new SM_LOJF());

		DataSet<SolutionMapping> sm14 = dataset
			.filter(new T2T_FF(null, "http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/vocabulary/rating2", null))
			.map(new T2SM_MF("?review", null, "?rating2"));

		DataSet<SolutionMapping> sm15 = sm13.leftOuterJoin(sm14)
			.where(new SM_JKS(new String[]{"?review"}))
			.equalTo(new SM_JKS(new String[]{"?review"}))
			.with(new SM_LOJF());

		DataSet<SolutionMapping> sm16 = dataset
			.filter(new T2T_FF(null, "http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/vocabulary/rating3", null))
			.map(new T2SM_MF("?review", null, "?rating3"));

		DataSet<SolutionMapping> sm17 = sm15.leftOuterJoin(sm16)
			.where(new SM_JKS(new String[]{"?review"}))
			.equalTo(new SM_JKS(new String[]{"?review"}))
			.with(new SM_LOJF());

		DataSet<SolutionMapping> sm18 = dataset
			.filter(new T2T_FF(null, "http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/vocabulary/rating4", null))
			.map(new T2SM_MF("?review", null, "?rating4"));

		DataSet<SolutionMapping> sm19 = sm17.leftOuterJoin(sm18)
			.where(new SM_JKS(new String[]{"?review"}))
			.equalTo(new SM_JKS(new String[]{"?review"}))
			.with(new SM_LOJF());

		DataSet<SolutionMapping> sm20;
		Node node = sm19.collect().get(0).getValue("?reviewDate");
		if(node.isLiteral()) {
			if(node.getLiteralValue().getClass().equals(BigDecimal.class) || node.getLiteralValue().getClass().equals(Double.class)){
				sm20 = sm19
					.sortPartition(new SM_OKS_Double("?reviewDate"), Order.DESCENDING)
					.setParallelism(1);
			} else if (node.getLiteralValue().getClass().equals(BigInteger.class) || node.getLiteralValue().getClass().equals(Integer.class)) {
				sm20 = sm19
					.sortPartition(new SM_OKS_Integer("?reviewDate"), Order.DESCENDING)
					.setParallelism(1);
			} else if (node.getLiteralValue().getClass().equals(Float.class)) {
				sm20 = sm19
					.sortPartition(new SM_OKS_Float("?reviewDate"), Order.DESCENDING)
					.setParallelism(1);
			} else if (node.getLiteralValue().getClass().equals(Long.class)){
				sm20 = sm19
					.sortPartition(new SM_OKS_Long("?reviewDate"), Order.DESCENDING)
					.setParallelism(1);
			} else {
				sm20 = sm19
					.sortPartition(new SM_OKS_String("?reviewDate"), Order.DESCENDING)
					.setParallelism(1);
			}
		} else {
				sm20 = sm19
					.sortPartition(new SM_OKS_String("?reviewDate"), Order.DESCENDING)
					.setParallelism(1);
		}

		DataSet<SolutionMapping> sm21 = sm20
			.map(new SM2SM_PF(new String[]{"?title", "?text", "?reviewDate", "?reviewer", "?reviewerName", "?rating1", "?rating2", "?rating3", "?rating4"}));

		DataSet<SolutionMapping> sm22 = sm21
			.first(20);

		//************ Sink  ************
		sm22.writeAsText(params.get("output")+"Query-Flink-Result", FileSystem.WriteMode.OVERWRITE)
			.setParallelism(1);

		env.execute("SPARQL Query to Flink Programan - DataSet API");
	}
}